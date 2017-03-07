<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" indent="yes" doctype-system="game.dtd" /> 
  
    <!-- identity transform, copy everything -->
	<xsl:template match="@*|node()">
	   <xsl:copy>
	      <xsl:apply-templates select="@*|node()"/>
	   </xsl:copy>
	</xsl:template>


    <!-- change ipcs to PUs in cost elements -->
	<xsl:template match="cost">
	   <xsl:copy>
			<xsl:attribute name="resource">PUs</xsl:attribute>   
			<xsl:copy-of select="@quantity"/>
	   </xsl:copy>
	</xsl:template>
	
	<!-- change IPCs to PUs in resoure elements -->
	<xsl:template match="resource">
	   <xsl:copy>
			<xsl:attribute name="name">PUs</xsl:attribute>   
	   </xsl:copy>
	</xsl:template>

	<!-- change IPCs to PUs in resourceGiven elements -->
	<xsl:template match="resourceGiven">
	   <xsl:copy>
	   		<xsl:copy-of select="@player"/>
			<xsl:attribute name="resource">PUs</xsl:attribute>   
			<xsl:copy-of select="@quantity"/>
	   </xsl:copy>
	</xsl:template>

    <!-- TODO - we need to preserve the CDATA sections in game notes -->
    <!--xsl:template match="property[@name='notes']">
       <property name="notes">
       		<xsl:text disable-output-escaping="yes">
				<xsl:value-of select="."/>
			</xsl:text>
       </property>
    </xsl:template-->	
    
	
</xsl:stylesheet>