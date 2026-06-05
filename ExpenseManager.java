import java.util.*;
import java.io.*;

public class ExpenseManager {

    private ArrayList<Expense> expenses = new ArrayList<>();
    private int nextId = 1;
    private static final String FILE = "expenses.csv";

    public ExpenseManager() {
        loadFromFile();
    }

    public Expense addExpense(double amount, String category, String date, String desc) {
        Expense e = new Expense(nextId++, desc, amount, category, date);
        expenses.add(e);
        saveToFile();
        return e;
    }

    public ArrayList<Expense> getAllExpenses() {
        return expenses;
    }

    public boolean deleteExpense(int id) {
        for (int i = 0; i < expenses.size(); i++) {
            if (expenses.get(i).id == id) {
                expenses.remove(i);
                saveToFile();
                return true;
            }
        }
        return false;
    }

    public Expense findById(int id) {
        for (Expense e : expenses) {
            if (e.id == id) return e;
        }
        return null;
    }

    public boolean updateExpense(int id, double amount, String category, String date, String desc) {
        Expense e = findById(id);
        if (e == null) return false;
        if (amount > 0)       e.amount = amount;
        if (category != null) e.cat    = category;
        if (date != null)     e.date   = date;
        if (desc != null)     e.desc   = desc;
        saveToFile();
        return true;
    }

    public ArrayList<Expense> search(String keyword) {
        ArrayList<Expense> result = new ArrayList<>();
        for (Expense e : expenses) {
            if (e.desc.toLowerCase().contains(keyword.toLowerCase()) ||
                e.cat.toLowerCase().contains(keyword.toLowerCase())) {
                result.add(e);
            }
        }
        return result;
    }

    public ArrayList<Expense> getByCategory(String cat) {
        ArrayList<Expense> result = new ArrayList<>();
        for (Expense e : expenses) {
            if (e.cat.equalsIgnoreCase(cat)) result.add(e);
        }
        return result;
    }

    public String getSummary() {
        Statistics stats   = new Statistics(expenses);
        double total       = stats.getTotalSpent();
        double avg         = stats.getAverageExpense();
        Expense highest    = stats.getHighestExpense();
        double max         = highest != null ? highest.amount : 0;
        String topCategory = stats.getTopCategory();

        Map<String, Double> catTotals = stats.getCategoryTotals();
        String[] cats = {"Food","Travel","Academic","Personal","Bills","Other"};
        StringBuilder catJson = new StringBuilder();
        for (int i = 0; i < cats.length; i++) {
            double catTotal = catTotals.getOrDefault(cats[i], 0.0);
            catJson.append("\"").append(cats[i]).append("\":").append(catTotal);
            if (i < cats.length - 1) catJson.append(",");
        }

        return "{" +
            "\"count\":"         + expenses.size() + "," +
            "\"total\":"         + total            + "," +
            "\"avg\":"           + (int) avg        + "," +
            "\"max\":"           + max              + "," +
            "\"topCategory\":\"" + topCategory      + "\"," +
            "\"byCategory\":{"   + catJson          + "}" +
        "}";
    }

    public String checkBudget(double budget) {
        Statistics stats = new Statistics(expenses);
        double total     = stats.getTotalSpent();
        double remaining = budget - total;
        boolean isOver   = total > budget;
        double  pct      = budget > 0 ? (total / budget) * 100 : 0;
        return "{" +
            "\"budget\":"     + budget    + "," +
            "\"totalSpent\":" + total     + "," +
            "\"remaining\":"  + remaining + "," +
            "\"isOver\":"     + isOver    + "," +
            "\"percentage\":" + (int) pct +
        "}";
    }

    public String listToJson(ArrayList<Expense> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            for (Expense e : expenses) pw.println(e.toText());
        } catch (IOException ex) {
            System.out.println("Save error: " + ex.getMessage());
        }
    }

    private void loadFromFile() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",", 5);
                if (p.length < 5) continue;
                int    id     = Integer.parseInt(p[0].trim());
                double amount = Double.parseDouble(p[1].trim());
                String cat    = p[2].trim();
                String date   = p[3].trim();
                String desc   = p[4].trim();
                expenses.add(new Expense(id, desc, amount, cat, date));
                if (id >= nextId) nextId = id + 1;
            }
        } catch (Exception ex) {
            System.out.println("Load error: " + ex.getMessage());
        }
    }
}
