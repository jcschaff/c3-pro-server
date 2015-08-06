package org.bch.c3pro.server.servlet;

import org.bch.c3pro.server.config.AppConfig;
import org.bch.c3pro.server.exception.C3PROException;
import org.bch.c3pro.server.external.S3Access;
import org.bch.c3pro.server.util.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: CH176656
 * Date: 8/5/15
 * Time: 10:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class USMap extends HttpServlet {
    private static final String [] classes = {".xx", ".q1", ".q5", ".q20", ".q100"};
    private static final int [] thresholds = {0,1,5,10,20};
    private static final String [] colors = {"fill: #E6F5EB;", "fill: #99D6AD;", "fill: #4DB870;", "fill: #009933;",
            "fill: #006B24;"};
    private static final String REPLACEMENT_WORD = "/*REPLACE_BY_C3PRO_CSS*/";

    protected S3Access s3 = new S3Access();
    public static final String BASE_MAP_FILE_NAME = "baseMap.svg";
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("image/svg-xml");
        StringBuffer sb = new StringBuffer();
        Utils.textFileToStringBuffer(USMap.class, BASE_MAP_FILE_NAME, sb, "\n");
        String svg = sb.toString();
        try {
            svg = colorMap(svg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.getWriter().println(svg);
        return;
    }

    private String colorMap(String svg) throws C3PROException, JSONException {
        // Grab the count
        String mapCount = s3.get(AppConfig.getProp(AppConfig.APP_FILENAME_MAPCOUNT));
        JSONObject jsonMapCount = new JSONObject(mapCount);
        long total = jsonMapCount.getLong(Utils.TOTAL_LABEL);
        // We avoid division by 0!
        if (total==0) return svg;

        // initialize the classes map
        Map<String, String> classesMap = new HashMap<>();
        for (int i=0; i < classes.length; i++) {
            classesMap.put(classes[i], classes[i]);
        }

        // classify
        for (int i=0; i<Utils.US_STATES.length; i++) {
            long stateCount = jsonMapCount.getLong(Utils.US_STATES[i]) * 100;
            long pct = stateCount / total;
            String cl = getClass((int)pct);
            String aux = classesMap.get(cl);
            aux = aux + ", ." + Utils.US_STATES[i];
            classesMap.put(cl, aux);
        }

        // generate CSS
        String css = "/*CSS file generated by c3pro server*/\n";
        for (int i=0; i < classes.length; i++) {
            String clhead = classesMap.get(classes[i]);
            String aux = clhead + "\n" + "{ " + colors[i] + " }\n";
            css = css + aux;
        }

        // place css in svg file
        return svg.replace(REPLACEMENT_WORD, css);

    }

    private String getClass(int pct) {
        String out = null;
        for (int i=0; i <thresholds.length; i++) {
            if (thresholds[i] >= pct) {
                out = classes[i];
            }
        }
        return out;
    }
}
