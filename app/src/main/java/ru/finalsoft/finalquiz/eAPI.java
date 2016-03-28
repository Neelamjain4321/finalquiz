package ru.finalsoft.finalquiz;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Random;

public class eAPI {
    private static eAPI instance = null;
    private String cookie = "";


    public static eAPI getInstance() {
        if (instance == null) {
            instance = new eAPI();
        }
        return instance;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie == null ? "" : cookie;
    }

    public JSONArray getCats() throws IOException, JSONException {
        return request("GET", "test/cats?api=1", "").getJSONArray("values");
    }

    public JSONObject getQuizzes(String category, int page) throws IOException, JSONException {
        return request("GET", "tests/" + category + "/page" + page + "?api=1", "");
    }

    public String getUnfinishedQuizIds() throws IOException, JSONException {
        String res = request("GET", "test/unfinished?api=1", "").getJSONArray("tests").toString();
        return res.length() > 2 ? res.substring(1, res.length() - 1) : "";
    }

    public JSONObject getQuiz(int test_id) throws IOException, JSONException {
        return request("GET", "test/" + test_id + "?api=1", "");
    }

    public JSONObject getUserResults(int page) throws IOException, JSONException {
        return request("GET", "myresults/page" + page + "?api=1", "");
    }

    public JSONObject getAccountInfo() throws IOException, JSONException {
        return request("POST", "user", "action=getuser");
    }

    public JSONObject postUserResult(int test_id, String answers) throws IOException,
            JSONException {
        String params = "api=1&id=" + test_id + "&answers=1&";

        return request("POST", "test/last/" + test_id, params + answers);
    }

    public void postUserAnswer(int test_id, QuizQuestion q) throws IOException, JSONException {
        String answers = "";

        switch (q.type) {
            case 0:
                for (int i = 0; i < q.quizQuestionAnswerArrayList.size(); i++)
                    if (q.quizQuestionAnswerArrayList.get(i).checked) {
                        answers += "&user_answer%5B%5D=" + i;
                        break;
                    }
                break;
            case 1:
                for (int i = 0; i < q.quizQuestionAnswerArrayList.size(); i++)
                    if (q.quizQuestionAnswerArrayList.get(i).checked)
                        answers += "&user_answer%5B%5D=" + i;
                break;
            case 2:
                try {
                    answers = "&user_answer=" + URLEncoder.encode(q.textAnswer, "UTF-8");
                } catch (Exception e) {
                    System.out.println(e.toString());
                }
                break;
        }

        if (answers.equals(""))
            throw new IOException();
        else
            request("POST", "save_answer/" + test_id, "question_id=" + q.question_id + answers);
    }

    public JSONObject doLogin(String email, String password) throws IOException, JSONException {
        return request("POST", "user", "action=dologin&login=" + email +
                "&password=" + password + "&api=1&remember=1");
    }

    public JSONObject doRegister(String email, String password) throws IOException,
            JSONException {
        return request("POST", "user", "action=doreg&user_email=" + email +
                "&user_name=" + email.substring(0, (email.indexOf("@") > 3) ? email.indexOf
                ("@") : 0) + new Random().nextInt(10000) + "&first_name=" + email.substring(0,
                (email.indexOf("@") > 3) ? email.indexOf("@") : 0) + "&password=" + password + "&api=1");
    }

    public JSONObject request(String method, String path, String params)
            throws IOException, JSONException {
        URL url = new URL("http://ecms.mbcsoft.ru/" + path);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod(method);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Cookie", cookie);
        conn.setRequestProperty("content-type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, "UTF-8"));
        writer.write(params);
        writer.close();
        wr.close();

        List<String> cookieList = conn.getHeaderFields().get("Set-Cookie");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null)
            response.append(inputLine);

        in.close();

        if (cookieList != null)
            for (String cookieItem : cookieList) {
                cookie += cookieItem.split(";", 2)[0] + "; ";
            }

        JSONObject d = new JSONObject(response.toString());

        d.put("cookie", cookie);

        return d;
    }
}
