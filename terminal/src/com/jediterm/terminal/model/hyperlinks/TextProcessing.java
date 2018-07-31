package com.jediterm.terminal.model.hyperlinks;

import com.jediterm.terminal.HyperlinkStyle;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.model.CharBuffer;
import com.jediterm.terminal.model.TerminalLine;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author traff
 */
public class TextProcessing {
  private final List<HyperlinkFilter> myHyperlinkFilter;
  private TextStyle myHyperlinkColor;
  private HyperlinkStyle.HighlightMode myHighlightMode;

  public TextProcessing(TextStyle hyperlinkColor, HyperlinkStyle.HighlightMode highlightMode) {
    myHyperlinkColor = hyperlinkColor;
    myHighlightMode = highlightMode;
    myHyperlinkFilter = new ArrayList<>();
  }

  public void processHyperlinks(TerminalLine line) {
    if (myHyperlinkFilter.isEmpty()) return;
    line.runWithLock(() -> {
      String lineStr = line.getText();
      for (HyperlinkFilter filter : myHyperlinkFilter) {
        LinkResult result = filter.apply(lineStr);
        if (result != null) {
          for (LinkResultItem item : result.getItems()) {
            TextStyle style = new HyperlinkStyle(myHyperlinkColor.getForeground(), myHyperlinkColor.getBackground(), item.getLinkInfo()).withHighlightMode(myHighlightMode);
            if (item.getStartOffset() >= 0 && item.getEndOffset() <= lineStr.length()) {
              line.writeString(item.getStartOffset(), new CharBuffer(lineStr.substring(item.getStartOffset(), item.getEndOffset())), style);
            }
          }
        }
      }
    });
  }

  public void addHyperlinkFilter(@NotNull HyperlinkFilter filter) {
    myHyperlinkFilter.add(filter);
  }
}
