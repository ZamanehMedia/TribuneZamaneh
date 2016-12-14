package com.tribunezamaneh.rss;

import android.content.Context;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;

import com.tinymission.rss.Item;

import org.apache.commons.lang.StringUtils;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import info.guardianproject.securereader.HTMLToPlainTextFormatter;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.ui.ContentFormatter;
import info.guardianproject.securereaderinterface.ui.UICallbacks;

/**
 * Created by N-Pex on 16-04-27.
 */
public class HTMLContentFormatter extends HTMLToPlainTextFormatter implements ContentFormatter {

    @Override
    public CharSequence getPlainText(Element element) {
        FormattingVisitor formatter = new FormattingVisitor();
        NodeTraversor traversor = new NodeTraversor(formatter);
        traversor.traverse(element); // walk the DOM, and call .head() and .tail() for each node
        return formatter.getContent();
    }

    @Override
    public CharSequence getFormattedItemContent(final Context context, Item item) {
        CharSequence cleanContent = item.getFormattedMainContent(this);

        // Trim white at beginning of text
        if (!TextUtils.isEmpty(cleanContent)) {
            int i = 0;
            while (i < cleanContent.length() &&
                    (Character.isWhitespace(cleanContent.charAt(i)) ||
                            Character.isSpaceChar(cleanContent.charAt(i)))) {
                i++;
            }
            if (i != 0) {
                cleanContent = cleanContent.subSequence(i, cleanContent.length());
            }
        }

        HTMLLinkSpan.LinkListener clickListener = null;

        // Linkify the links!
        if (cleanContent instanceof Spannable) {
            Spannable s = (Spannable) cleanContent;
            Object[] spans = s.getSpans(0, cleanContent.length(), HTMLContentFormatter.HTMLLinkSpan.class);
            for (Object span : spans) {
                HTMLContentFormatter.HTMLLinkSpan link = (HTMLContentFormatter.HTMLLinkSpan) span;
                if (clickListener == null) {
                    clickListener = new HTMLLinkSpan.LinkListener() {
                        @Override
                        public void onLinkClicked(String url) {
                            Bundle params = new Bundle();
                            params.putString("url", url);
                            UICallbacks.handleCommand(context, R.integer.command_read_more, params);
                        }
                    };
                }
                link.setListener(clickListener);
            }
        }
        return cleanContent;
    }

    // the formatting rules, implemented in a breadth-first DOM traverse
    private class FormattingVisitor implements NodeVisitor {
        private SpannableStringBuilder accum = new SpannableStringBuilder(); // holds the accumulated text
        private int startOfLink = -1;
        private String ignoreUntilElement;
        private int ignoreUntilDepth;

        // hit when the node is first seen
        public void head(Node node, int depth) {
            String name = node.nodeName();
            if (ignoreUntilElement != null)
                return;
            if (node instanceof TextNode) {
                String text = ((TextNode) node).text();
                // Replace LINE SEPARATOR char
                text = text.replace("\u2028", "\r\n");
                // Replace PARAGRAPH SEPARATOR char
                text = text.replace("\u2029", "\r\n\r\n");
                append(text); // TextNodes carry all user-readable text in the DOM.
            }
            else if (name.equals("li"))
                append("\n * ");
            else if (name.equals("dt"))
                append("  ");
            else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr"))
                append("\n");
            else if (name.equals("a"))
                startOfLink = accum.length();
            else if (name.equals("div") && node instanceof Element) {
                Element element = (Element) node;
                if (element.hasAttr("class") && element.attr("class").contains("wp-caption")) {
                    ignoreUntilElement = "div";
                    ignoreUntilDepth = depth;
                }
            }
        }

        // hit when all of the node's children (if any) have been visited
        public void tail(Node node, int depth) {
            String name = node.nodeName();
            if (ignoreUntilElement != null && name != null && name.contentEquals(ignoreUntilElement) && depth == ignoreUntilDepth) {
                ignoreUntilElement = null;
            } else if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5"))
                append("\n");
            else if (name.equals("a")) {
                if (startOfLink != -1) {
                    //Uncomment this to get clickable links
                    HTMLLinkSpan span = new HTMLLinkSpan(node.absUrl("href"));
                    accum.setSpan(span, startOfLink, accum.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    //String url = span.getURL();
                    //append(" <");
                    //append(url);
                    //span = new HTMLLinkSpan(span.getURL());
                    //accum.setSpan(span, accum.length() - url.length(), accum.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    //append(">");
                    startOfLink = -1;
                }
            }
        }

        // appends text to the string builder with a simple word wrap method
        private void append(CharSequence text) {
            accum.append(text);
        }

        public CharSequence getContent() {
            return accum;
        }
    }

    public static class HTMLLinkSpan extends ClickableSpan {
        public interface LinkListener {
            void onLinkClicked(String url);
        }
        private LinkListener listener;
        private String url;
        public HTMLLinkSpan(String url) {
            this.url = url;
        }

        @Override
        public void onClick(View widget) {
            if (listener != null)
                listener.onLinkClicked(this.url);
        }

        public void setListener(LinkListener listener) {
            this.listener = listener;
        }

        public String getURL() {
            return this.url;
        }
    }
}
