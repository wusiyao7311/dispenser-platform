const API_BASE = "http://localhost:8080/api";

const tableBody = document.querySelector("#dispenser-table tbody");
const banner = document.getElementById("low-stock-banner");
const lowStockCount = document.getElementById("low-stock-count");
const refreshBtn = document.getElementById("refresh-btn");
const form = document.getElementById("dispenser-form");
const formMessage = document.getElementById("form-message");

async function fetchJson(path, options) {
    const response = await fetch(`${API_BASE}${path}`, options);
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || `Request failed: ${response.status}`);
    }
    if (response.status === 204) {
        return null;
    }
    return response.json();
}

function statusPill(status) {
    return `<span class="status-pill status-${status}">${status}</span>`;
}

function renderStockCell(stockLevels) {
    if (!stockLevels.length) {
        return '<span class="stock-item">no stock recorded</span>';
    }
    return stockLevels
        .map((s) => {
            const low = s.capacity > 0 && s.quantity / s.capacity <= 0.2;
            return `<span class="stock-item ${low ? "low" : ""}">${s.productSku}: ${s.quantity}/${s.capacity}</span>`;
        })
        .join("");
}

async function loadDispensers() {
    const [dispensers, lowStock] = await Promise.all([
        fetchJson("/dispensers"),
        fetchJson("/dispensers/low-stock"),
    ]);

    if (lowStock.length > 0) {
        lowStockCount.textContent = lowStock.length;
        banner.hidden = false;
    } else {
        banner.hidden = true;
    }

    const stockByDispenser = new Map();
    for (const s of lowStock) {
        if (!stockByDispenser.has(s.dispenserId)) stockByDispenser.set(s.dispenserId, []);
    }

    const rows = await Promise.all(
        dispensers.map(async (d) => {
            const stock = await fetchJson(`/dispensers/${d.id}/stock`);
            return `
                <tr>
                    <td>${d.code}</td>
                    <td>${d.location}</td>
                    <td>${statusPill(d.status)}</td>
                    <td>${renderStockCell(stock)}</td>
                </tr>
            `;
        })
    );

    tableBody.innerHTML = rows.join("");
}

refreshBtn.addEventListener("click", () => loadDispensers().catch(showError));

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(form).entries());

    formMessage.textContent = "";
    formMessage.className = "message";

    try {
        await fetchJson("/dispensers", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data),
        });
        formMessage.textContent = `Dispenser "${data.code}" created.`;
        formMessage.className = "message success";
        form.reset();
        await loadDispensers();
    } catch (err) {
        formMessage.textContent = err.message;
        formMessage.className = "message error";
    }
});

function showError(err) {
    tableBody.innerHTML = `<tr><td colspan="4">Failed to load dispensers: ${err.message}. Is the API running on :8080?</td></tr>`;
}

loadDispensers().catch(showError);
