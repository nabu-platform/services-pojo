package be.nabu.libs.services.pojo;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "pojo")
public class POJOConfiguration {
	
	private String className;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
}
