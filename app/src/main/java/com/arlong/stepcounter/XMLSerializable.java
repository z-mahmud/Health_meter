/*
 * Created on Dec 20, 2011
 * Author: Paul Woelfel
 * Email: frig@frig.at
 */
package com.arlong.stepcounter;

import org.w3c.dom.Element;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public interface XMLSerializable {
	public void serialize(XmlSerializer serializer)  throws RuntimeException, IOException;
	public void deserialize(Element e);
}
