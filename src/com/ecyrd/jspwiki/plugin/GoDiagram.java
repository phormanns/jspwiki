/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.plugin;

import org.apache.log4j.Category;
import com.ecyrd.jspwiki.*;
import java.util.*;

import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.IOException;

/**
 *  Plugin for doing go game diagrams.
 *
 *  @author Janne Jalkanen
 */
public class GoDiagram
    implements WikiPlugin
{
    private static Category log = Category.getInstance( GoDiagram.class );

    public static final int BLACK_FIRST = 1;
    public static final int WHITE_FIRST = 0;

    public void initialize( WikiContext context, Map params )
        throws PluginException
    {
    }

    private String makeImage( String content )
    {
        //return "<IMG SRC=\"images/diagram/"+content+".gif\">";
        return "<TD><IMG SRC=\"images/diagram/"+content+".gif\"></TD>";
    }

    public class DiagramInfo
    {
        boolean topSide = false;
        boolean bottomSide = false;
        boolean leftSide = false;
        boolean rightSide = false;

        int numRows = 0;
        int numCols = 0;

        Object[][] contents;
    }

    protected DiagramInfo getDiagramInfo( String dia )
    {
        DiagramInfo info = new DiagramInfo();
        StringTokenizer tok = new StringTokenizer( dia.trim(), "\n" );

        ArrayList rows = new ArrayList();

        while( tok.hasMoreTokens() )
        {
            String line = tok.nextToken().trim();

            int firstBar = line.indexOf('|');
            int lastBar  = line.lastIndexOf('|');

            if( firstBar == 0 )
                info.leftSide = true;

            if( lastBar == (line.length()-1) )
                info.rightSide = true;

            int firstDash = line.indexOf('-');
            if( firstDash >= 0 )
            {
                // Top or bottom row

                if( info.numRows == 0 )
                    info.topSide = true;
                else
                    info.bottomSide = true;
            }
            else
            {
                // Actual diagram row.
                // Gobble it all in, don't bother to parse.

                ArrayList       currentRow = new ArrayList();
                StringTokenizer st2        = new StringTokenizer( line, " " );

                info.numRows++;

                while( st2.hasMoreTokens() )
                {
                    String mark = st2.nextToken();

                    if( mark.equals("|") ) continue;

                    currentRow.add( mark );
                }

                info.numCols = currentRow.size();
                rows.add( currentRow );
            }                        
        }

        info.contents = new String[info.numRows][info.numCols];

        /*
        System.out.println("\n");
        System.out.println( (info.topSide ? "top " : " ")+
                            (info.bottomSide ? "bottom ": " ")+
                            (info.leftSide ? "left " : " ")+
                            (info.rightSide ? "right " : " " ) );
        */

        for( int i = 0; i < info.numRows; i++ )
        {
            for( int j = 0; j < info.numCols; j++ )
            {
                info.contents[i][j] = (String) ((ArrayList)rows.get(i)).get(j);
                //System.out.print( info.contents[i][j] );
            }
            //System.out.print("\n");
        }
        return info;
    }

    /**
     *  @param first 'b', if black should have the first move, 'w' otherwise.
     */
    private String parseDiagram( String dia, int first )
        throws IOException
    {
        DiagramInfo info = getDiagramInfo( dia );

        // System.out.println("dia="+dia);

        StringBuffer res = new StringBuffer();

        //res.append("<DIV CLASS=\"diagram\">\n");
        res.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\" CELLPADDING=\"0\">");
        int type;

        if( info.topSide )
        {
            res.append("<TR>");

            if( info.leftSide ) res.append( makeImage("ULC") );
            for( int i = 0; i < info.numCols; i++ )
            {
                res.append( makeImage("TS") );
            }
            if( info.rightSide ) res.append( makeImage("URC") );
            res.append("</TR>\n");
        }

        res.append("<TR>");

        for( int row = 0; row < info.numRows; row++ )
        {
            if( info.leftSide ) res.append( makeImage("LS") );

            for( int col = 0; col < info.numCols; col++ )
            {
                String item = (String)info.contents[row][col];

                if( Character.isDigit(item.charAt(0)) )
                {
                    int num = Integer.parseInt( item );

                    String which = (num % 2 == first) ? "b" : "w";
                    res.append( makeImage( which+Integer.toString(num) ) );
                    continue;
                }

                switch( item.charAt(0) )
                {
                  case '#':
                  case 'X':
                    res.append( makeImage("b") );
                    break;

                  case 'O':
                    res.append( makeImage("w") );
                    break;

                  case '.':
                    res.append( makeImage("empty") );
                    break;

                  case ',':
                    res.append( makeImage("hoshi") );
                    break;

                  default:
                    res.append( makeImage( "lc"+item ) );
                    break;
                }
            } // col

            if( info.rightSide ) res.append( makeImage("RS") );
            res.append("</TR>\n<TR>");
        } // row

        res.append("</TR>\n");

        if( info.bottomSide )
        {
            res.append("<TR>");
            if( info.leftSide ) res.append( makeImage("LLC") );
            for( int i = 0; i < info.numCols; i++ )
            {
                res.append( makeImage("BS") );
            }
            if( info.rightSide ) res.append( makeImage("LRC") );
            res.append("</TR>\n");
        }

        //res.append("</DIV>\n");
        res.append("</TABLE>");

        return res.toString();
    }

    // FIXME: Parameters should be checked against HTML entities.
    // FIXME: "label" should be run through parser

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        String diagram = (String) params.get( "_body" );
        String label   = (String) params.get( "label" );
        String first   = (String) params.get( "first" );

        if( diagram == null || diagram.length() == 0 )
        {
            return "<B>No diagram detected.</B>";
        }

        if( first == null || first.length() == 0 || 
            !(first.startsWith("w") || first.startsWith("W")) )
        {
            first = "b";
        }

        try
        {
            StringBuffer sb = new StringBuffer();

            sb.append("<table border=1 align=left style=\"margin: 10px;\">");
            sb.append("<tr><td>\n");
            sb.append( parseDiagram( diagram, 
                                     (first.startsWith("b") ? BLACK_FIRST : WHITE_FIRST )) );
            sb.append("</td></tr>\n");
            if( label != null )
            {
                sb.append( "<tr><td class=\"diagramlabel\">Dia: "+label+"</td></tr>\n" );
            }
            sb.append("</table>\n");

            return sb.toString();
        }
        catch( IOException e )
        {
            log.error("Something funny in diagram", e );

            throw new PluginException("Error in diagram: "+e.getMessage());
        }
    }
}
