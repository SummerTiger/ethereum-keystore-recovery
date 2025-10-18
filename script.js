// Ethereum Keystore Recovery Tool - Frontend JavaScript
// Smooth scrolling, animations, and interactive features

document.addEventListener('DOMContentLoaded', function() {
    // Smooth scrolling for navigation links
    initSmoothScrolling();

    // Terminal typing animation
    initTerminalAnimation();

    // Scroll animations
    initScrollAnimations();

    // Copy code to clipboard
    initCodeCopy();
});

/**
 * Initialize smooth scrolling for anchor links
 */
function initSmoothScrolling() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            const href = this.getAttribute('href');

            // Skip if href is just "#"
            if (href === '#') return;

            e.preventDefault();
            const target = document.querySelector(href);

            if (target) {
                const headerOffset = 80;
                const elementPosition = target.getBoundingClientRect().top;
                const offsetPosition = elementPosition + window.pageYOffset - headerOffset;

                window.scrollTo({
                    top: offsetPosition,
                    behavior: 'smooth'
                });
            }
        });
    });
}

/**
 * Terminal typing animation effect
 */
function initTerminalAnimation() {
    const terminal = document.querySelector('.terminal-body');
    if (!terminal) return;

    const lines = terminal.querySelectorAll('.terminal-line');

    // Hide all lines initially
    lines.forEach(line => {
        line.style.opacity = '0';
    });

    // Animate lines one by one
    let delay = 0;
    lines.forEach((line, index) => {
        setTimeout(() => {
            line.style.transition = 'opacity 0.3s ease';
            line.style.opacity = '1';
        }, delay);

        // Variable delay based on line type
        if (line.classList.contains('terminal-success')) {
            delay += 800; // Longer delay before success message
        } else {
            delay += 400;
        }
    });
}

/**
 * Initialize scroll-triggered animations
 */
function initScrollAnimations() {
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -100px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('animate-in');
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    // Observe feature cards
    document.querySelectorAll('.feature-card').forEach(card => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(30px)';
        card.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
        observer.observe(card);
    });

    // Observe steps
    document.querySelectorAll('.step').forEach(step => {
        step.style.opacity = '0';
        step.style.transform = 'translateX(-30px)';
        step.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
        observer.observe(step);
    });

    // Observe doc cards
    document.querySelectorAll('.doc-card').forEach(card => {
        card.style.opacity = '0';
        card.style.transform = 'scale(0.95)';
        card.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
        observer.observe(card);
    });
}

/**
 * Add "Copy to Clipboard" functionality to code blocks
 */
function initCodeCopy() {
    // Add copy buttons to code examples
    document.querySelectorAll('.code-example, .download-code').forEach(codeBlock => {
        const button = document.createElement('button');
        button.className = 'copy-button';
        button.innerHTML = `
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
            </svg>
            Copy
        `;

        button.addEventListener('click', () => {
            const code = codeBlock.querySelector('code');
            if (code) {
                copyToClipboard(code.textContent);
                showCopyFeedback(button);
            }
        });

        // Position button
        codeBlock.style.position = 'relative';
        button.style.position = 'absolute';
        button.style.top = '8px';
        button.style.right = '8px';
        button.style.padding = '6px 12px';
        button.style.background = 'rgba(99, 102, 241, 0.1)';
        button.style.border = '1px solid rgba(99, 102, 241, 0.3)';
        button.style.borderRadius = '6px';
        button.style.color = '#818cf8';
        button.style.fontSize = '12px';
        button.style.fontWeight = '500';
        button.style.cursor = 'pointer';
        button.style.display = 'flex';
        button.style.alignItems = 'center';
        button.style.gap = '4px';
        button.style.transition = 'all 0.2s ease';

        button.addEventListener('mouseenter', () => {
            button.style.background = 'rgba(99, 102, 241, 0.2)';
        });

        button.addEventListener('mouseleave', () => {
            button.style.background = 'rgba(99, 102, 241, 0.1)';
        });

        codeBlock.appendChild(button);
    });
}

/**
 * Copy text to clipboard
 */
function copyToClipboard(text) {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(text).catch(err => {
            console.error('Failed to copy:', err);
            fallbackCopy(text);
        });
    } else {
        fallbackCopy(text);
    }
}

/**
 * Fallback copy method for older browsers
 */
function fallbackCopy(text) {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();

    try {
        document.execCommand('copy');
    } catch (err) {
        console.error('Fallback copy failed:', err);
    }

    document.body.removeChild(textarea);
}

/**
 * Show visual feedback when code is copied
 */
function showCopyFeedback(button) {
    const originalHTML = button.innerHTML;

    button.innerHTML = `
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
        Copied!
    `;
    button.style.background = 'rgba(34, 197, 94, 0.2)';
    button.style.borderColor = 'rgba(34, 197, 94, 0.3)';
    button.style.color = '#22c55e';

    setTimeout(() => {
        button.innerHTML = originalHTML;
        button.style.background = 'rgba(99, 102, 241, 0.1)';
        button.style.borderColor = 'rgba(99, 102, 241, 0.3)';
        button.style.color = '#818cf8';
    }, 2000);
}

/**
 * Add animation class when element is in view
 */
document.addEventListener('DOMContentLoaded', () => {
    const style = document.createElement('style');
    style.textContent = `
        .animate-in {
            opacity: 1 !important;
            transform: translateY(0) translateX(0) scale(1) !important;
        }
    `;
    document.head.appendChild(style);
});

/**
 * Stats counter animation
 */
function animateCounter(element, target, duration = 2000) {
    let current = 0;
    const increment = target / (duration / 16);
    const timer = setInterval(() => {
        current += increment;
        if (current >= target) {
            element.textContent = target;
            clearInterval(timer);
        } else {
            element.textContent = Math.floor(current);
        }
    }, 16);
}

/**
 * Initialize stats animation when in view
 */
function initStatsAnimation() {
    const statsObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const valueElement = entry.target.querySelector('.stat-value');
                const text = valueElement.textContent;

                // Extract number from text (e.g., "20k-50k" -> 50, "96%" -> 96)
                const match = text.match(/\d+/);
                if (match) {
                    const number = parseInt(match[0]);
                    animateCounter(valueElement, number);
                }

                statsObserver.unobserve(entry.target);
            }
        });
    }, { threshold: 0.5 });

    document.querySelectorAll('.stat').forEach(stat => {
        statsObserver.observe(stat);
    });
}

// Initialize stats animation
document.addEventListener('DOMContentLoaded', initStatsAnimation);

/**
 * Header scroll effect
 */
let lastScroll = 0;
window.addEventListener('scroll', () => {
    const header = document.querySelector('.header');
    const currentScroll = window.pageYOffset;

    if (currentScroll > 100) {
        header.style.boxShadow = '0 4px 6px -1px rgba(0, 0, 0, 0.3)';
    } else {
        header.style.boxShadow = 'none';
    }

    lastScroll = currentScroll;
});

/**
 * Mobile navigation toggle (for future mobile menu)
 */
function toggleMobileMenu() {
    const nav = document.querySelector('.nav');
    nav.classList.toggle('nav-open');
}

/**
 * Terminal auto-scroll effect
 */
function initTerminalAutoScroll() {
    const terminal = document.querySelector('.terminal-body');
    if (!terminal) return;

    setInterval(() => {
        const scrollHeight = terminal.scrollHeight;
        const height = terminal.clientHeight;
        const maxScroll = scrollHeight - height;

        if (maxScroll > 0) {
            terminal.scrollTop = maxScroll;
        }
    }, 100);
}

// Initialize terminal auto-scroll
document.addEventListener('DOMContentLoaded', initTerminalAutoScroll);

/**
 * Add keyboard shortcuts
 */
document.addEventListener('keydown', (e) => {
    // Ctrl/Cmd + K to focus search (if implemented)
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        // Future: Focus search input
    }

    // Escape to close modals (if implemented)
    if (e.key === 'Escape') {
        // Future: Close any open modals
    }
});

/**
 * Performance monitoring (optional)
 */
if (window.performance && window.performance.timing) {
    window.addEventListener('load', () => {
        const perfData = window.performance.timing;
        const pageLoadTime = perfData.loadEventEnd - perfData.navigationStart;
        console.log(`Page load time: ${pageLoadTime}ms`);
    });
}

/**
 * Add external link indicators
 */
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('a[target="_blank"]').forEach(link => {
        link.setAttribute('rel', 'noopener noreferrer');
    });
});
