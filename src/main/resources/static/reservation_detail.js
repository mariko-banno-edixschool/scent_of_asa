(() => {
  const feedback = document.querySelector("#reservation-status-feedback");

  const labels = {
    confirmed: "予約確定",
    paid: "支払済み",
    "checked-in": "来店済み",
    cancelled: "キャンセル",
    "no-show": "無断不来店",
    pending: "調整中",
  };

  const classes = {
    confirmed: "",
    paid: "is-paid",
    "checked-in": "is-done",
    cancelled: "is-alert",
    "no-show": "is-stop",
    pending: "is-waiting",
  };

  document.querySelectorAll(".reservation-status-save").forEach((button) => {
    button.addEventListener("click", () => {
      const row = button.closest("tr");
      const select = row.querySelector(".reservation-status-select");
      const badge = row.querySelector(".reservation-status-badge");
      const reservationId = row.children[0]?.textContent?.trim() || "";
      const status = select.value;

      badge.textContent = labels[status] || status;
      badge.dataset.status = status;
      badge.className = `admin-badge reservation-status-badge ${classes[status] || ""}`.trim();

      if (feedback) {
        feedback.textContent = `${reservationId} のステータスを「${labels[status]}」に更新しました。`;
      }
    });
  });
})();
