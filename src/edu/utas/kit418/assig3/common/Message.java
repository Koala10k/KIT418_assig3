package edu.utas.kit418.assig3.common;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import org.json.simple.JSONObject;

public class Message implements Serializable {
	private static final long serialVersionUID = 7175566041485696382L;
	public UUID msgID;

	public Message() {
		expiredIn = Double.MAX_VALUE;
		msgID = UUID.randomUUID();
		buildTime = new Date().getTime();
	}

	public String from;
	public String owner;
	public int type = -1;
	// 0: NodeInfo
	// 1:ClientTaskRst
	// 2: NodeTaskRsp
	// 3: NodeStartup
	// 4: NodeReadyToWork
	// 5: ClientTaskStatusRst
	// 6: ServerAuthRst
	// 7: ClientAuthRsp
	// 8: ServerTaskStatusRsp
	// 9: ClientSysInfoRst
	// 10: ServerSysInfoRsp
	// 11: ClientCancelTaskRst
	// 12: ServerCancelTaskRsp
	// 13: ServerAuthRsp
	public String content;
	public String answer;

	public double buildTime;
	public double expiredIn;
	public NodeInfo nodeInfo;

	@SuppressWarnings("unchecked")
	public String toJsonString() {
		JSONObject jobj = new JSONObject();
		jobj.put("id", msgID);
		jobj.put("clientId", owner);
		jobj.put("type", type);
		jobj.put("content", content);
		jobj.put("answer", answer);
		jobj.put("expiredIn", expiredIn);
		jobj.put("nodeInfo", nodeInfo);
		return jobj.toJSONString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int hash = 1;
		hash = prime * hash + ((msgID == null) ? 0 : msgID.hashCode());
		hash = prime * hash + ((owner == null) ? 0 : owner.hashCode());
		hash = prime * hash + ((content == null) ? 0 : content.hashCode());
		hash = prime * hash + ((answer == null) ? 0 : answer.hashCode());
		hash = prime * hash + new Double(expiredIn).hashCode();
		hash = prime * hash + ((nodeInfo == null) ? 0 : nodeInfo.hashCode());
		hash = prime * hash + type;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		return msgID.equals(((Message) obj).msgID);
	}

	@Override
	public String toString() {
		if(type != 1 && type != 2) return super.toString();
		return  content+"                "+(expiredIn==Double.MAX_VALUE?"unlimited":(expiredIn+"s"));
	}

	
}
