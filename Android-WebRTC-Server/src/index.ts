import { WebSocketServer } from "ws";
import Room from "./room";

const mWs = new WebSocketServer({ port: 8000 });
const mRoom = new Room();

console.log("Server started on port 8080");
console.log(mWs.address());

mWs.on("connection", (ws) => {
  console.log("Connect Success");
  mRoom.connect(ws);
});
