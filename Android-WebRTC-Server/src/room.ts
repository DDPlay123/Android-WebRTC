import WebSocket from "ws";
import { v4 as uuidV4 } from "uuid";

// 定義事件
enum MessageType {
  JOIN_ROOM = "joinRoom",
  LEAVE_ROOM = "leaveRoom",
  OFFER_ANSWER = "offerAnswer",
  CANDIDATE = "candidate",
}

// 定義 signaling message
type PeerMessage =
  | JoinRoomMessage
  | LeaveRoomMessage
  | OfferAnswerMessage
  | CandidateMessage;

type JoinRoomMessage = {
  type: MessageType.JOIN_ROOM;
  remoteId: string;
  offer: boolean;
};

type LeaveRoomMessage = {
  type: MessageType.LEAVE_ROOM;
};

type OfferAnswerMessage = {
  type: MessageType.OFFER_ANSWER;
};

type CandidateMessage = {
  type: MessageType.CANDIDATE;
};

type Peer = {
  // 自己的 Id
  id: string;
  // WebSocket
  ws: WebSocket;
  // 目標的 Id
  remoteId?: string;
};

// signaling message 處理
export default class Room {
  // 使用者列表
  private peers: Map<string, Peer>;
  // 第一個進來房間的使用者 Id
  private firstPeerId: string;

  constructor() {
    this.peers = new Map();
    this.firstPeerId = "";
  }

  // 連接使用者
  connect(ws: WebSocket) {
    const id = uuidV4();
    const peer = { id, ws };
    console.log(id);
    // 新增至使用者列表
    this.peers.set(id, peer);
    this.onJoinIn(peer);

    ws.on("close", () => this.onLeave(id));
    ws.on("error", () => this.onLeave(id));
    ws.on("message", (data: WebSocket.Data) =>
      this.handleMessage(id, data.toString())
    );
  }

  // Client發送的訊息處理，把當前使用者 currentUser 發的訊息轉發給對方使用者 peerUser
  private handleMessage(id: string, data: string) {
    try {
      console.log(`Client Message: ${data}`);
      const message = JSON.parse(data) as PeerMessage;
      // 尋找當前的使用者(自己)
      const currentPeer = this.peers.get(id);
      if (!currentPeer) {
        return console.log(`Not fount current user ${id}`);
      }
      // 尋找目標使用者
      const remotePeer = this.peers.get(currentPeer.remoteId);
      if (!remotePeer) {
        return console.log(`Not fount remote user ${id}`);
      }
      // 事件處理
      switch (message.type) {
        case MessageType.OFFER_ANSWER:
          this.send(remotePeer, message);
          break;

        case MessageType.CANDIDATE:
          this.send(remotePeer, message);
          break;

        case MessageType.LEAVE_ROOM:
          this.send(remotePeer, message);
          this.onLeave(currentPeer.id);
          break;

        default:
          console.log(`Unknow Error ${id}: ${data}`);
          break;
      }
    } catch (err) {
      console.log(`Error ${id}: ${data}`);
    }
  }

  // 加入房間事件處理
  private onJoinIn(currPeer: Peer) {
    if (this.firstPeerId != "") {
      // 尋找目標使用者 Id
      const findPeer = this.peers.get(this.firstPeerId);
      if (findPeer) {
        currPeer.remoteId = this.firstPeerId;
        findPeer.remoteId = currPeer.id;

        // 發送給自己，自己作為 offer 方
        this.send(currPeer, {
          type: MessageType.JOIN_ROOM,
          remoteId: findPeer.id,
          offer: true,
        });

        // 發送給對方，對方作為 answer 方
        this.send(findPeer, {
          type: MessageType.JOIN_ROOM,
          remoteId: currPeer.id,
          offer: false,
        });
      }
    } else {
      // 第一個進來房間的使用者
      this.firstPeerId = currPeer.id;
    }
  }

  // 離開房間事件處理
  private onLeave(id: string) {
    console.log(`User leave: ${id}`);
    const findPeer = this.peers.get(id);
    if (findPeer && findPeer.remoteId) {
      const peer = this.peers.get(findPeer.remoteId);
      if (peer) {
        this.send(peer, { type: MessageType.LEAVE_ROOM });
      }
    }
    if (id == this.firstPeerId) {
      this.firstPeerId = "";
    }
    this.peers.delete(id);
  }

  // 訊息發送事件處理
  private send(peer: Peer, message: PeerMessage) {
    try {
      // 判斷是否處理連接狀態
      if (peer.ws.readyState === WebSocket.OPEN) {
        peer.ws.send(JSON.stringify(message));
      }
    } catch (err) {
      console.log(`Send Error: ${peer.id}`);
    }
  }
}
