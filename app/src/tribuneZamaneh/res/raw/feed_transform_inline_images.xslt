<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
              xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
              xmlns:atom="http://www.w3.org/2005/Atom"
              xmlns:content="http://purl.org/rss/1.0/modules/content/"
              xmlns:media="http://search.yahoo.com/mrss/"
              version="1.0">
  <xsl:output method="xml" version="1.0" encoding="UTF-8"
              indent="no"
              media-type="application/rss+xml" />

  <xsl:template match="@*|node()">
    <xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy>
  </xsl:template>

  <xsl:template match="content:encoded">
    <xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy>
    <xsl:for-each select="text()[contains(., '&lt;img')]">
        <xsl:call-template name="tokenize">
            <xsl:with-param name="pText" select="."/>
        </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="tokenize">
        <xsl:param name="pText"/>
        <xsl:if test="contains($pText, '&lt;img')">
            <xsl:variable name="afteropen" select="substring-after($pText,'&lt;img')" />
            <xsl:variable name="body" select="substring-before($afteropen, '&gt;')" />
            <xsl:variable name="srcstart" select="substring-after($body,'src=&quot;')" />
            <xsl:variable name="src" select="substring-before($srcstart,'&quot;')" />
            <media:content>
                <xsl:attribute name="url">
                    <xsl:value-of select="$src" />
                </xsl:attribute>
            </media:content>
            <xsl:call-template name="tokenize">
                <xsl:with-param name="pText" select="$afteropen"/>
            </xsl:call-template>
        </xsl:if>
  </xsl:template>
</xsl:stylesheet>
