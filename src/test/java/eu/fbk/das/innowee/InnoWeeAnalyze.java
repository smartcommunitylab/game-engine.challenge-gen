package eu.fbk.das.innowee;

import eu.fbk.das.rs.challenges.ChallengesBaseTest;


import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.ext.PointConcept;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static eu.fbk.das.rs.utils.Utils.*;

public class InnoWeeAnalyze extends ChallengesBaseTest {

    // String gameId = "5c9b48cad9aedcf3d418b936"; - gandhi

    String gameId = "5dd2ac42bec030000187f762"; // S. Pellico
    // String gameId = "5dd2aca7bec030000187f774"; // Arcivescovile

    String[] keys = new String[] {"totalReduce", "totalReuse", "totalRecycle"};

    String period = "R2";

    String token = "9d9b2075-7e19-4494-a162-30a04b27779f";

    Map<String, String> schools;

    String[] coins = new String[] {"reduce", "reuse", "recycle"};

    // ottenere token: https://am-dev.smartcommunitylab.it/aac/eauth/authorize?client_id=2be89b9c-4050-4e7e-9042-c02b0d9121c6&redirect_uri=http://localhost:2020/aac&response_type=token

    // swagger https://innoweee.platform.smartcommunitylab.it/innoweee-engine/swagger-ui.html



    private Set<String> pIds;

    private Map<String, Set<String>> robotPieces;

    Map<String, Map<String, Map<String,  Map<String,Map<String,Integer>>>>> mapGame;
    private Map<String, Map<String, Map<String, Map<String, Integer>>>> mapClass;
    private Map<String, Map<String, Map<String, Integer>>> mapPlayer;
    private Map<String, Map<String, Integer>> mapAct;
    private Map<String, Integer> mapWeek;

    private Map<String, Map<String, Integer>> mapState;
    private Map<String, Integer> state;

    @Test
    public void analyze() throws IOException, ParseException {

        schools = new HashMap<>();
        schools.put("5dd2ac42bec030000187f762", "pellico");
        schools.put("5dd2aca7bec030000187f774", "arcivescovile");

        mapState = new TreeMap<>();

        analyzeGame("5dd2ac42bec030000187f762");
        analyzeGameActions("TRENTINO");
        p(pIds);

        /*
        for (String gameId: mapGame.keySet()) {
            pf("### gameId: %s \n", gameId);
            mapClass = mapGame.get(gameId);
            for (String classId: mapClass.keySet()) {
                pf("# classId: %s \n", classId);
                mapPlayer = mapClass.get(classId);

                if (!mapPlayer.containsKey("build_robot"))
                    continue;

                mapAct = mapPlayer.get("build_robot");

                for (String weekId: mapAct.keySet()) {
                    pf("# weekId: %s \n", weekId);
                    p(mapAct.get(weekId).keySet());
                }
            }
        }*/

        p("\n \t PER RACCOLTA \n");

        for (String gameId: mapGame.keySet()) {
            pf("### school: %s \n", schools.get(gameId));
            mapClass = mapGame.get(gameId);
            for (String classId: mapClass.keySet()) {
                pf("%s \t", classId);
                mapPlayer = mapClass.get(classId);

                if (!mapPlayer.containsKey("build_robot")) {
                    p("");
                    continue;
                }

                mapAct = mapPlayer.get("build_robot");

                for (int i = 1; i < 7; i++) {
                    String weekId = f("R%d", i);
                    int v = 0;
                    if (mapAct.containsKey(weekId))
                        v = mapAct.get(weekId).keySet().size();
                    pf("%d \t", v);
                }
                p("");
            }
        }

        p("\n \t PER LIVELLO \n");

        for (String gameId: mapGame.keySet()) {
            pf("### school: %s \n",  schools.get(gameId));
            mapClass = mapGame.get(gameId);
            for (String classId: mapClass.keySet()) {
                pf("%s \t", classId);
                mapPlayer = mapClass.get(classId);

                if (!mapPlayer.containsKey("build_robot")) {
                    p("");
                    continue;
                }

                mapAct = mapPlayer.get("build_robot");

                int lvl1 = 0;
                int lvl2 = 0;
                int lvl3 = 0;

                for (int i = 1; i < 7; i++) {
                    String weekId = f("R%d", i);

                    if (!mapAct.containsKey(weekId))
                        continue;

                    mapWeek = mapAct.get(weekId);
                    for (String piecId: mapAct.get(weekId).keySet()) {
                        int v = mapWeek.get(piecId);
                        if (v == 1) lvl1 ++;
                        else if (v == 2) lvl2++;
                        else if (v == 3) lvl3++;
                    }
                }
                pf("%d \t %d \t %d \n", lvl1, lvl2, lvl3);
            }
        }

        p("\n \t PER TIPO \n");

        for (String gameId: mapGame.keySet()) {
            pf("### school: %s \n",  schools.get(gameId));
            mapClass = mapGame.get(gameId);
            for (String classId: mapClass.keySet()) {
                pf("%s \t", classId);
                mapPlayer = mapClass.get(classId);

                if (!mapPlayer.containsKey("build_robot")) {
                    p("");
                    continue;
                }

                mapAct = mapPlayer.get("build_robot");

                Map<String, Integer> type = new HashMap<>();

                for (int i = 1; i < 7; i++) {
                    String weekId = f("R%d", i);

                    if (!mapAct.containsKey(weekId))
                        continue;

                    mapWeek = mapAct.get(weekId);
                    for (String piecId: mapAct.get(weekId).keySet()) {
                        String tp = piecId.substring(0, piecId.indexOf("-"));
                        if (!type.containsKey(tp))
                            type.put(tp, 0);

                        type.put(tp, type.get(tp) + 1);
                    }
                }

                for (String tp: new String[] {"head", "chest", "armL", "armR", "legs"}) {
                    int v = 0;
                    if (type.containsKey(tp)) v = type.get(tp);
                    pf("%d \t", v);
                }

                p("");
            }
        }

        for (int i = 1; i < 6; i++) {
            String weekId = f("R%d", i);

            pf("\n \t RACCOLTA: %s \n", weekId);

            for (String gameId: mapGame.keySet()) {
                pf("### school: %s \n",  schools.get(gameId));
                mapClass = mapGame.get(gameId);
                for (String classId: mapClass.keySet()) {
                    pf("%s \t", classId);
                    mapPlayer = mapClass.get(classId);

                    if (!mapPlayer.containsKey("add_point")) {
                        p("");
                        continue;
                    }

                    mapAct = mapPlayer.get("add_point");
                    if (!mapAct.containsKey(weekId))
                            continue;
                    mapWeek = mapAct.get(weekId);

                    for (String tp: coins) {
                        int v = 0;
                        if (mapWeek.containsKey(tp)) v = mapWeek.get(tp);
                        pf("%d \t", v);
                    }

                    p("");
                }
            }
        }

        p("\n \t STATO \n");

        for (String gameId: mapGame.keySet()) {
            pf("### school: %s \n",  schools.get(gameId));
            mapClass = mapGame.get(gameId);
            for (String classId: mapClass.keySet()) {
                pf("%s \t", classId);
                mapPlayer = mapClass.get(classId);

                String k = gameId + "-" + classId;

                state = mapState.get(k);

                Map<String, Integer> type = new HashMap<>();

                for (String c: coins) {
                    int v = 0;
                    if (state.containsKey(c)) v = state.get(c);
                    pf("%d \t", v);
                }

                p("");
            }
        }
    }

    private void analyzeGameActions(String s) {

        String res = executeRestCall("gameaction/%s", s);
        if (res == null)
            return;

        try {
            JSONParser parser = new JSONParser();
            JSONArray json = (JSONArray) parser.parse(res);

            mapGame = new HashMap<>();

            Iterator<JSONObject> iterator = json.iterator();
            while (iterator.hasNext()) {
                JSONObject ob = iterator.next();
                analyzeAction(ob);
            }

        } catch (ParseException e) {
            // TODO
            e.printStackTrace();
        }

    }

    private void analyzeAction(JSONObject ob) {

        String gId = (String) ob.get("gameId");
        if (!mapGame.containsKey(gId))
            mapGame.put(gId, new TreeMap<>());
        mapClass = mapGame.get(gId);

        String pNm = (String) ob.get("playerName");
        if (!mapClass.containsKey(pNm))
            mapClass.put(pNm, new TreeMap<>());
        mapPlayer = mapClass.get(pNm);

        String acT = (String) ob.get("actionType");
        if (!mapPlayer.containsKey(acT))
            mapPlayer.put(acT, new TreeMap<>());
        mapAct = mapPlayer.get(acT);

        Long creation = (Long) ob.get("creationDate");
        DateTime creatDate = new DateTime(creation);
        int week = (daysApart(parseDate("02/12/2019"), creatDate) / 7) + 1;
        // TODO remove in other istances
        if (week > 3) week-=2;
        String weekName = f("R%d", week);
        if (!mapAct.containsKey(weekName))
            mapAct.put(weekName, new TreeMap<>());
        mapWeek = mapAct.get(weekName);

        String k = gId + "-" + pNm;
        if (!mapState.containsKey(k ))
            mapState.put(k, new TreeMap<>());
        state = mapState.get(k);

        if (acT.equals("build_robot")) {
            analyzeBuild(ob);
        } else  if (acT.equals("add_point")) {
            analyzeAdd(ob);
        }  else  if (acT.equals("add_contribution")) {
            analyzeContribution(ob, weekName);
        } else  if (acT.equals("add_altruistic"))
            pf("UNKNOW ACTION %s \n", acT);

        // p(ob);
    }

    private void analyzeContribution(JSONObject ob, String weekName) {

        Long creation = (Long) ob.get("creationDate");
        DateTime creatDate = new DateTime(creation);

        pf("Contribution, class: %s (%s), on %s (%s) \n", ob.get("playerName"), schools.get(ob.get("gameId")), weekName, creatDate.toString());
    }

    private void analyzeAdd(JSONObject ob) {

        JSONObject cd = (JSONObject) ob.get("customData");
        String credit = "void";
        Integer v = 1;
        String pNm = (String) cd.get("pointType");
        if (pNm.equals("itemDelivery")) {
            Boolean reusable = (Boolean) cd.get("reusable");
            if (reusable)
                credit = "reuse";
            else
                credit = "recycle";
        } else if (pNm.equals("reduceReport")) {
            // p(ob);
            Double coin = (Double) cd.get("reduceCoin");
            v = coin.intValue();
            credit = "reduce";
        } else p("UNKNOWN pointType");

        if (!mapWeek.containsKey(credit))
            mapWeek.put(credit, 0);
        mapWeek.put(credit, mapWeek.get(credit) + v);

        if (!state.containsKey(credit))
            state.put(credit, 0);
        state.put(credit, state.get(credit) + v);
    }

    private void analyzeBuild(JSONObject ob) {
        JSONObject cd = (JSONObject) ob.get("customData");
        String cNm = (String) cd.get("componentId");
        mapWeek.put(cNm, StringUtils.countMatches(cNm, "-") - 1);
        for (String c: coins) {
            Integer v = ((Double) cd.get(c + "Coin")).intValue();
            if (!state.containsKey(c))
                state.put(c, 0);
            state.put(c, state.get(c) - v);
        }
    }

    public void analyzeGame(String gameId) {

        String res = executeRestCall("player/%s", gameId);
        if (res == null)
            return;

        try {
            JSONParser parser = new JSONParser();
            JSONArray json = (JSONArray) parser.parse(res);

             pIds = new HashSet<>();

             robotPieces = new HashMap<>();

            Iterator<JSONObject> iterator = json.iterator();
            while (iterator.hasNext()) {
                JSONObject ob = iterator.next();
                analyzePlayer(ob);

            }

        } catch (ParseException e) {
            // TODO
            e.printStackTrace();
        }

    }

    private void analyzePlayer(JSONObject ob) {
        String nm = (String) ob.get("name");
        if (nm.equals("Scuola"))
            return;

        pIds.add(nm);

        Set<String> pieces = new HashSet<>();

        JSONObject robot = (JSONObject) ob.get("robot");
        JSONObject comp = (JSONObject) robot.get("components");
        for (Object p: comp.keySet()) {
            pieces.add((String) p);
        }

        robotPieces.put(nm, pieces);
    }

    public String executeRestCall(String format, Object... args) {
        try {
            return performRestCall(format, args);
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }

        return null;
    }

    public String performRestCall(String format, Object... args) throws IOException {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        String path = f(format, args);
        HttpGet httpget = new HttpGet(f("https://innoweee.platform.smartcommunitylab.it/innoweee-engine/api/%s", path));
        httpget.setHeader("Authorization", "Bearer " + token);
        HttpResponse httpresponse = httpclient.execute(httpget);

        Scanner sc = new Scanner(httpresponse.getEntity().getContent());

        StatusLine res = httpresponse.getStatusLine();
        p(res);

        //Printing the status line
        if (res.toString().equals("HTTP/1.1 200 OK")) {
            StringBuilder sb = new StringBuilder();
            while (sc.hasNext()) {
                sb.append(sc.nextLine());
            }

            return sb.toString();
        }

        p("ERRORE IN CHIAMATA! HAI MESSO IL TOKEN NUOVO? https://am-dev.smartcommunitylab.it/aac/eauth/authorize?client_id=2be89b9c-4050-4e7e-9042-c02b0d9121c6&redirect_uri=http://localhost:2020/aac&response_type=token");

        return null;
    }


    public void execute() {
        Set<String> res = facade.getGamePlayers(gameId);
        for (String pId: res) {
            pf("%s ", pId);
        }

        p("\n");

        for (String pId: res) {
            PlayerStateDTO st = facade.getPlayerState(gameId, pId);
            String[] values = new String[3];
            Set<GameConcept> scores =  st.getState().get("PointConcept");
            for (GameConcept gc : scores) {
                PointConcept pc = (PointConcept) gc;

                // p(pc.getName());
                int pos = pos(pc.getName(), keys);
                if (pos < 0)
                    continue;

                PointConcept.Period aux = pc.getPeriod(period);

                values[pos] = f("%.2f", pc.getScore());
            }

            pf("%s,%s\n", pId, String.join(",", values));
        }

    }

    private int pos(String name, String[] keys) {
        for (int i = 0; i < keys.length; i++)
            if (keys[i].equals(name))
                return i;

            return -1;
    }
}
