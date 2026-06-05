public class Expense {

    public int    id;
    public String desc;
    public double amount;
    public String cat;
    public String date;

    public Expense(int id, String desc, double amount, String cat, String date) {
        this.id     = id;
        this.desc   = desc;
        this.amount = amount;
        this.cat    = cat;
        this.date   = date;
    }

    public int    getId()          { return id; }
    public String getDescription() { return desc; }
    public double getAmount()      { return amount; }
    public String getCategory()    { return cat; }
    public String getDate()        { return date; }

    public void setDescription(String desc) { this.desc   = desc; }
    public void setAmount(double amount)    { this.amount = amount; }
    public void setCategory(String cat)     { this.cat    = cat; }
    public void setDate(String date)        { this.date   = date; }

    public String toText() {
        return id + "," + amount + "," + cat + "," + date + "," + desc;
    }

    public String toJson() {
        return "{\"id\":"      + id +
               ",\"desc\":\""  + desc   + "\"" +
               ",\"amount\":" + amount +
               ",\"cat\":\""   + cat    + "\"" +
               ",\"date\":\""  + date   + "\"}";
    }
}
