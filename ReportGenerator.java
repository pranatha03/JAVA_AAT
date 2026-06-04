import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReportGenerator {

    // Total expenses
    public static double getTotalExpenses(ArrayList<Expense> expenses) {
        double total = 0;

        for (Expense e : expenses) {
            total += e.amount;
        }

        return total;
    }

    // Average expense
    public static double getAverageExpense(ArrayList<Expense> expenses) {

        if (expenses.isEmpty()) {
            return 0;
        }

        return getTotalExpenses(expenses) / expenses.size();
    }

    // Highest expense
    public static Expense getHighestExpense(ArrayList<Expense> expenses) {

        if (expenses.isEmpty()) {
            return null;
        }

        Expense highest = expenses.get(0);

        for (Expense e : expenses) {

            if (e.amount > highest.amount) {
                highest = e;
            }
        }

        return highest;
    }

    // Category-wise totals
    public static Map<String, Double> getCategoryBreakdown(
            ArrayList<Expense> expenses) {

        Map<String, Double> breakdown = new HashMap<>();

        for (Expense e : expenses) {

            breakdown.put(
                    e.cat,
                    breakdown.getOrDefault(e.cat, 0.0) + e.amount
            );
        }

        return breakdown;
    }

    // Monthly report
    public static Map<String, Double> getMonthlyReport(
            ArrayList<Expense> expenses) {

        Map<String, Double> monthly = new HashMap<>();

        for (Expense e : expenses) {

            // YYYY-MM
            String month = e.date.substring(0, 7);

            monthly.put(
                    month,
                    monthly.getOrDefault(month, 0.0) + e.amount
            );
        }

        return monthly;
    }

    // Text summary report
    public static String generateSummary(ArrayList<Expense> expenses) {

        StringBuilder sb = new StringBuilder();

        sb.append("===== EXPENSE REPORT =====\n");
        sb.append("Total Expenses: ")
          .append(getTotalExpenses(expenses))
          .append("\n");

        sb.append("Average Expense: ")
          .append(getAverageExpense(expenses))
          .append("\n");

        Expense highest = getHighestExpense(expenses);

        if (highest != null) {

            sb.append("Highest Expense: ")
              .append(highest.desc)
              .append(" (")
              .append(highest.amount)
              .append(")\n");
        }

        sb.append("\nCategory Breakdown:\n");

        Map<String, Double> categories =
                getCategoryBreakdown(expenses);

        for (String cat : categories.keySet()) {

            sb.append(cat)
              .append(": ")
              .append(categories.get(cat))
              .append("\n");
        }

        return sb.toString();
    }
}
