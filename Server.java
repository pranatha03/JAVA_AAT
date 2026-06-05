import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    static ExpenseManager manager       = new ExpenseManager();
    static BudgetTracker  budgetTracker = new BudgetTracker();

    public static void main(String[] args) throws Exception {
        // Railway uses PORT env variable — falls back to 8080 locally
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isEmpty()) {
            port = Integer.parseInt(envPort);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/expenses", new ExpenseHandler());
        server.createContext("/api/summary",  new SummaryHandler());
        server.createContext("/api/budget",   new BudgetHandler());
        server.createContext("/api/sort",     new SortHandler());
        server.createContext("/api/stats",    new StatsHandler());
        server.createContext("/api/health",   new HealthHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("✅ Backend running on port " + port);
    }

    static void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        ex.getResponseHeaders().add("Content-Type",                 "application/json");
    }

    static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        addCorsHeaders(ex);
        byte[] bytes = json.getBytes("UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    static String readBody(HttpExchange ex) throws IOException {
        Scanner sc = new Scanner(ex.getRequestBody(), "UTF-8");
        String body = sc.hasNext() ? sc.useDelimiter("\\A").next() : "";
        sc.close();
        return body;
    }

    static String parseJson(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return "";
        int start = idx + search.length();
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return json.substring(start + 1, end);
        } else {
            int end = json.indexOf(',', start);
            if (end == -1) end = json.indexOf('}', start);
            return json.substring(start, end).trim();
        }
    }

    static class ExpenseHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod();
            String path   = ex.getRequestURI().getPath();
            String query  = ex.getRequestURI().getQuery();

            if (method.equals("OPTIONS")) { sendJson(ex, 200, "{}"); return; }

            if (method.equals("GET")) {
                if (query != null && query.startsWith("cat=")) {
                    sendJson(ex, 200, manager.listToJson(manager.getByCategory(query.split("=")[1])));
                    return;
                }
                if (query != null && query.startsWith("search=")) {
                    sendJson(ex, 200, manager.listToJson(manager.search(query.split("=")[1])));
                    return;
                }
                sendJson(ex, 200, manager.listToJson(manager.getAllExpenses()));
                return;
            }

            if (method.equals("POST")) {
                String body   = readBody(ex);
                String desc   = parseJson(body, "desc");
                String amtStr = parseJson(body, "amount");
                String cat    = parseJson(body, "cat");
                String date   = parseJson(body, "date");
                if (amtStr.isEmpty() || desc.isEmpty()) {
                    sendJson(ex, 400, "{\"error\":\"amount and desc required\"}");
                    return;
                }
                Expense e = manager.addExpense(Double.parseDouble(amtStr), cat, date, desc);
                sendJson(ex, 200, e.toJson());
                return;
            }

            if (method.equals("DELETE")) {
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                boolean done = manager.deleteExpense(id);
                sendJson(ex, done ? 200 : 404,
                    done ? "{\"message\":\"Deleted\"}" : "{\"message\":\"Not found\"}");
                return;
            }

            if (method.equals("PUT")) {
                int    id     = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                String body   = readBody(ex);
                String amtStr = parseJson(body, "amount");
                String cat    = parseJson(body, "cat");
                String date   = parseJson(body, "date");
                String desc   = parseJson(body, "desc");
                double amount = amtStr.isEmpty() ? -1 : Double.parseDouble(amtStr);
                boolean updated = manager.updateExpense(id, amount,
                    cat.isEmpty()  ? null : cat,
                    date.isEmpty() ? null : date,
                    desc.isEmpty() ? null : desc);
                sendJson(ex, updated ? 200 : 404,
                    updated ? "{\"message\":\"Updated\"}" : "{\"message\":\"Not found\"}");
            }
        }
    }

    static class SummaryHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (ex.getRequestMethod().equals("OPTIONS")) { sendJson(ex, 200, "{}"); return; }
            sendJson(ex, 200, manager.getSummary());
        }
    }

    static class BudgetHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod();
            String path   = ex.getRequestURI().getPath();
            if (method.equals("OPTIONS")) { sendJson(ex, 200, "{}"); return; }
            if (method.equals("POST") && path.endsWith("/set")) {
                String body  = readBody(ex);
                String cat   = parseJson(body, "cat");
                String limit = parseJson(body, "limit");
                if (!cat.isEmpty() && !limit.isEmpty()) {
                    budgetTracker.setBudget(cat, Double.parseDouble(limit));
                    sendJson(ex, 200, "{\"message\":\"Budget set\"}");
                } else {
                    sendJson(ex, 400, "{\"error\":\"cat and limit required\"}");
                }
                return;
            }
            String query  = ex.getRequestURI().getQuery();
            double budget = (query != null && query.startsWith("amount=")) ?
                Double.parseDouble(query.split("=")[1]) : 0;
            sendJson(ex, 200, manager.checkBudget(budget));
        }
    }

    static class SortHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (ex.getRequestMethod().equals("OPTIONS")) { sendJson(ex, 200, "{}"); return; }
            String query = ex.getRequestURI().getQuery();
            ArrayList<Expense> sorted = (query != null && query.equals("by=date"))
                ? SortFilter.sortByDate(manager.getAllExpenses())
                : SortFilter.sortByAmount(manager.getAllExpenses());
            sendJson(ex, 200, manager.listToJson(sorted));
        }
    }

    static class StatsHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (ex.getRequestMethod().equals("OPTIONS")) { sendJson(ex, 200, "{}"); return; }
            Statistics stats   = new Statistics(manager.getAllExpenses());
            Expense    highest = stats.getHighestExpense();
            Expense    lowest  = stats.getLowestExpense();
            sendJson(ex, 200, "{" +
                "\"totalSpent\":"     + stats.getTotalSpent()    + "," +
                "\"avgExpense\":"     + stats.getAverageExpense() + "," +
                "\"topCategory\":\"" + stats.getTopCategory()    + "\"," +
                "\"highestExpense\":" + (highest != null ? highest.toJson() : "null") + "," +
                "\"lowestExpense\":"  + (lowest  != null ? lowest.toJson()  : "null") +
            "}");
        }
    }

    static class HealthHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (ex.getRequestMethod().equals("OPTIONS")) { sendJson(ex, 200, "{}"); return; }
            sendJson(ex, 200, "{\"status\":\"running\",\"message\":\"Java backend is up!\"}");
        }
    }
}
