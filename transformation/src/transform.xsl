<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ff="http://www.kulturarv.dk/fundogfortidsminder/ff">
    <xsl:output method="xml" indent="yes" />
    <xsl:template match="OAI-PMH">
        <xsl:apply-templates select="ListRecords/record/header"/>
    </xsl:template>

    <xsl:template match="header">
        <xsl:result-document href="pages/page{position()}.xml">
            <xsl:copy-of select="root(snapshot())"/>
        </xsl:result-document>
    </xsl:template>
</xsl:stylesheet>