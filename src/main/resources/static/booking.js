document.addEventListener("DOMContentLoaded", () => {
  const calendarCards = document.querySelectorAll(".calendar-card");

  const syncPressedState = (card) => {
    card.querySelectorAll(".day").forEach((day) => {
      day.setAttribute("aria-pressed", day.classList.contains("selected") ? "true" : "false");
    });
  };

  calendarCards.forEach((card) => {
    syncPressedState(card);

    card.addEventListener("click", (event) => {
      const day = event.target.closest(".day");
      if (!day || day.classList.contains("muted")) {
        return;
      }

      card.querySelectorAll(".day.selected").forEach((selectedDay) => {
        selectedDay.classList.remove("selected");
      });

      day.classList.add("selected");
      syncPressedState(card);
    });
  });
});
