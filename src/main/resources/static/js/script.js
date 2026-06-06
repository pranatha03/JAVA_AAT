const API_BASE = "https://javaaat-production.up.railway.app";

document.addEventListener("DOMContentLoaded", () => {

    const form = document.getElementById("expenseForm");

    if (form) {

        form.addEventListener("submit", async (e) => {

            e.preventDefault();

            const description =
                document.getElementById("description").value.trim();

            const amount =
                document.getElementById("amount").value.trim();

            const category =
                document.getElementById("category").value;

            if (!description || !amount) {
                alert("Please complete all fields.");
                return;
            }

            const expenseData = {
                amount: parseFloat(amount),
                cat: category,
                date: new Date().toISOString().split("T")[0],
                desc: description
            };

            try {

                const response = await fetch(
                    `${API_BASE}/api/expenses`,
                    {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify(expenseData)
                    }
                );

                if (response.ok) {

                    alert("Expense added successfully!");
                    form.reset();

                } else {

                    const error = await response.text();
                    alert("Failed: " + error);

                }

            } catch (err) {

                console.error(err);
                alert("Cannot connect to backend");

            }

        });

    }

});
