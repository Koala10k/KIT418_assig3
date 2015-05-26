package edu.utas.kit418.assig3.common;

import java.io.Serializable;

import org.hyperic.sigar.CpuPerc;
import org.json.simple.JSONObject;

public class NodeInfo implements Serializable {
	private static final long serialVersionUID = 7175566041485696381L;
	public double cpusPerc[];
	public double memPerc;
	public long memTotal;
	public String status;
	public int presIdx;

	public void print() {
		System.out.println(this.toJsonString());
	}

	@SuppressWarnings("unchecked")
	public String toJsonString() {
		JSONObject jObj = new JSONObject();
		for (int i = 0; i < cpusPerc.length; i++) {
			jObj.put("cpu" + i, CpuPerc.format(cpusPerc[i]));
		}
		jObj.put("mem%", String.format("%.2f", memPerc) + "%");
		jObj.put("mem total", (memTotal / 1024 / 1024) + "MB");
		jObj.put("status", status);
		jObj.put("pressure", presIdx);
		return jObj.toJSONString();
	}

	@SuppressWarnings("unchecked")
	public String jsonCpuInfo() {
		JSONObject jObj = new JSONObject();
		for (int i = 0; i < cpusPerc.length; i++) {
			jObj.put("cpu" + i, CpuPerc.format(cpusPerc[i]));
		}
		return jObj.toJSONString();
	}

	public void calcPresIndex() {
		double cpuIdx = 0, memIdx;
		for (int i = 0; i < cpusPerc.length; i++) {
			cpuIdx += cpusPerc[i] * 100;
		}
		cpuIdx /= cpusPerc.length;
		memIdx = memPerc;
		presIdx =  (int) ((cpuIdx + memIdx) * 0.5d);
	}

}
