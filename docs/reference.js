const outlineLinks = [...document.querySelectorAll('.outline a[href^="#"]')];
const outlineTargets = outlineLinks
  .map((link) => document.querySelector(link.getAttribute('href')))
  .filter(Boolean);
const navGroups = document.querySelectorAll('.outline details');
const compactNavigation = window.matchMedia('(max-width: 640px)');

const setNavigationMode = () => {
  navGroups.forEach((group) => { group.open = !compactNavigation.matches; });
};

setNavigationMode();
compactNavigation.addEventListener('change', setNavigationMode);

if ('IntersectionObserver' in window) {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      outlineLinks.forEach((link) => {
        link.classList.toggle('active', link.hash === `#${entry.target.id}`);
      });
    });
  }, { rootMargin: '-15% 0px -75% 0px' });
  outlineTargets.forEach((target) => observer.observe(target));
}
