package tsteelworks.client.pages;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TSTextPage extends TSBookPage
{
    String text;

    @Override
    public void readPageFromXML (Element element)
    {
        final NodeList nodes = element.getElementsByTagName("text");
        if (nodes != null)
            text = nodes.item(0).getTextContent();
    }

    @Override
    public void renderContentLayer (int localWidth, int localHeight)
    {
        manual.fonts.drawSplitString(text, localWidth, localHeight, 178, 0);
    }
}
