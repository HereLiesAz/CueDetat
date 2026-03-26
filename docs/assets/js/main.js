document.addEventListener('DOMContentLoaded', () => {
    /*
     * Scroll Animations using IntersectionObserver
     */
    const observerOptions = {
        root: document.querySelector('.parallax-wrapper'), // Observe relative to the parallax container
        rootMargin: '0px',
        threshold: 0.15
    };

    const observer = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                // Optional: Stop observing once animated in
                // observer.unobserve(entry.target);
            } else {
                // Optional: Remove class to re-animate on scroll up
                entry.target.classList.remove('visible');
            }
        });
    }, observerOptions);

    const animatedElements = document.querySelectorAll('.fade-in, .fade-in-left, .fade-in-right, .slide-up, .scale-up');
    animatedElements.forEach(el => observer.observe(el));

    /*
     * Dynamic Mouse Tilt for Showcase Image
     */
    const tiltElement = document.querySelector('.dynamic-tilt');
    const showcaseSection = document.querySelector('.showcase');

    if (tiltElement && showcaseSection) {
        showcaseSection.addEventListener('mousemove', (e) => {
            requestAnimationFrame(() => {
                const rect = tiltElement.getBoundingClientRect();

                // Calculate mouse position relative to the element's center
                const x = e.clientX - rect.left - rect.width / 2;
                const y = e.clientY - rect.top - rect.height / 2;

                // Sensitivity modifiers
                const multiplier = 0.05;

                // Apply 3D transform (rotateX inversely to Y, rotateY directly to X)
                tiltElement.style.transform = `perspective(1000px) rotateX(${-y * multiplier}deg) rotateY(${x * multiplier}deg) scale3d(1.05, 1.05, 1.05)`;
            });
        });

        // Reset transform on mouse leave
        showcaseSection.addEventListener('mouseleave', () => {
            tiltElement.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
        });
    }

    /*
     * Initial triggering for elements already in view (like Hero)
     */
    setTimeout(() => {
        const heroContent = document.querySelector('.hero-content');
        if(heroContent) heroContent.classList.add('visible');
    }, 100);
});
