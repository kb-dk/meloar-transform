<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output omit-xml-declaration="yes" indent="yes"/>
    <xsl:strip-space elements="*"/>
    <xsl:param name="pDest" select="'file:///c:/temp/'"/>
    <xsl:template match="*[starts-with(name(),'ExportXML')]">
        <xsl:for-each select="record">
            <xsl:result-document href="{$pDest}section{position()}.xml">
                <JobPositionPostings>
                    <JobPositionPosting>
                        <xsl:apply-templates select="*:field[starts-with(@name,'ContestNumber')]"/>
                        <JobDisplayOptions>
                            <xsl:apply-templates select="*:field[starts-with(@name,'ManagerRequisitionTitle')]"/>
                        </JobDisplayOptions>
                    </JobPositionPosting>
                </JobPositionPostings>
            </xsl:result-document>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>