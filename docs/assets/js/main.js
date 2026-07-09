document.addEventListener('DOMContentLoaded', () => {
    /*
     * Scroll Animations using IntersectionObserver
     */
    const observerOptions = {
        root: null, // Use viewport
        rootMargin: '0px',
        threshold: 0.15
    };

    const observer = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
            }
        });
    }, observerOptions);

    const animatedElements = document.querySelectorAll('.fade-in, .fade-in-left, .fade-in-right, .slide-up, .scale-up');
    animatedElements.forEach(el => observer.observe(el));

    /*
     * Navbar scroll effect
     */
    const navbar = document.getElementById('navbar');
    window.addEventListener('scroll', () => {
        if (window.scrollY > 50) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
    });

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

                // Apply 3D transform
                tiltElement.style.transform = `perspective(1000px) rotateX(${-y * multiplier}deg) rotateY(${x * multiplier}deg) scale3d(1.02, 1.02, 1.02)`;
            });
        });

        // Reset transform on mouse leave
        showcaseSection.addEventListener('mouseleave', () => {
            tiltElement.style.transform = 'perspective(1000px) rotateX(-15deg) rotateY(5deg) scale3d(1, 1, 1)';
        });
    }

    /*
     * Parallax Scrolling Effect
     */
    const parallaxElements = document.querySelectorAll('.parallax-bg, .parallax-fast, .parallax-slow');
    
    window.addEventListener('scroll', () => {
        const scrolled = window.scrollY;
        
        requestAnimationFrame(() => {
            parallaxElements.forEach(el => {
                let speed = 0.5; // default slow background parallax
                if (el.classList.contains('parallax-fast')) speed = 1.2;
                if (el.classList.contains('parallax-slow')) speed = 0.2;
                
                // For backgrounds, we might want slower, for floating elements, faster
                const yPos = -(scrolled * speed);
                el.style.transform = `translateY(${yPos}px)`;
            });
        });
    }, { passive: true });

    /*
     * Initial triggering for elements already in view (like Hero)
     */
    setTimeout(() => {
        const heroContent = document.querySelector('.hero-content');
        if(heroContent) heroContent.classList.add('visible');
    }, 100);
});
