const backdrop = document.querySelector<HTMLElement>('.backdrop')!;
const sideDrawer = document.querySelector<HTMLElement>('.mobile-nav')!;
const menuToggle = document.querySelector<HTMLElement>('#side-menu-toggle')!;

function backdropClickHandler(): void {
    backdrop.style.display = 'none';
    sideDrawer.classList.remove('open');
}

function menuToggleClickHandler(): void {
    backdrop.style.display = 'block';
    sideDrawer.classList.add('open');
}

backdrop.addEventListener('click', backdropClickHandler);
menuToggle.addEventListener('click', menuToggleClickHandler);
