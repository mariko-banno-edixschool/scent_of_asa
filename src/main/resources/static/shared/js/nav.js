document.addEventListener("DOMContentLoaded", () => {
  const navWraps = document.querySelectorAll(".nav-wrap");

  navWraps.forEach((navWrap) => {
    const toggle = navWrap.querySelector(".nav-toggle");
    const nav = navWrap.querySelector(".main-nav");

    if (!toggle || !nav) {
      return;
    }

    const closeMenu = () => {
      navWrap.classList.remove("is-open");
      toggle.setAttribute("aria-expanded", "false");
    };

    toggle.addEventListener("click", () => {
      const isOpen = navWrap.classList.toggle("is-open");
      toggle.setAttribute("aria-expanded", String(isOpen));
    });

    navWrap.querySelectorAll(".main-nav a, .header-actions a").forEach((link) => {
      link.addEventListener("click", () => {
        if (window.innerWidth <= 760) {
          closeMenu();
        }
      });
    });

    window.addEventListener("resize", () => {
      if (window.innerWidth > 760) {
        closeMenu();
      }
    });
  });
});
