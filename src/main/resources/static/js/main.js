$(document).ready(function(){
    $(".confirmar-ocupacion").click(function (event) {
        let idOcupacion = $(this).data("idocupacion");
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                confirmarOcupacion(idOcupacion);
                window.location.href = "/";
            }
        });


    });

    $(".anular-ocupacion").click(function (event) {
        let idOcupacion = $(this).data("idocupacion");
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                anularOcupacion(idOcupacion);
                window.location.href = "/";

            }
        });
    });

//    // Inicializar el carrusel
//    new bootstrap.Carousel(document.querySelector('#carrousel-lista-ocupaciones-pendientes'), {
//        interval: 5000, // Cambiar cada 5 segundos
//        wrap: true,     // Permitir ciclo continuo
//        touch: true     // Permitir control táctil
//    });
//
//    // Agregar animación a las tarjetas cuando se muestran
//    const carousel = document.querySelector('#carrousel-lista-ocupaciones-pendientes');
//    carousel.addEventListener('slide.bs.carousel', function () {
//        const cards = document.querySelectorAll('.card');
//        cards.forEach(card => {
//            card.classList.remove('animate__fadeIn');
//            void card.offsetWidth; // Forzar reflow
//            card.classList.add('animate__fadeIn');
//        });
//    });

    initializeGsapHomeAnimations();
    initializeHomeCounters();

});

function initializeHomeCounters() {
    const counters = Array.from(document.querySelectorAll(".js-home-counter"));
    if (!counters.length
        || !("IntersectionObserver" in window)
        || window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        return;
    }

    const formatCount = function (value) {
        return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ".");
    };

    const runCounter = function (counter) {
        const target = parseInt(counter.dataset.countTarget, 10);
        if (!Number.isFinite(target) || target <= 0) {
            return;
        }
        const duration = 1600;
        const start = performance.now();
        const step = function (now) {
            const progress = Math.min((now - start) / duration, 1);
            const eased = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);
            counter.textContent = formatCount(Math.round(target * eased));
            if (progress < 1) {
                requestAnimationFrame(step);
            }
        };
        counter.textContent = "0";
        requestAnimationFrame(step);
    };

    const observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting) {
                observer.unobserve(entry.target);
                runCounter(entry.target);
            }
        });
    }, { threshold: 0.4 });

    counters.forEach(function (counter) {
        observer.observe(counter);
    });
}

function initializeGsapHomeAnimations() {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        document.body.style.opacity = "1";
        return;
    }

    if (!window.gsap) {
        document.body.style.opacity = "1";
        return;
    }

    const hero = document.querySelector(".js-gsap-hero");
    const pendingCards = gsap.utils.toArray(".js-gsap-pending-card");

    document.body.style.opacity = "1";

    if (window.ScrollTrigger) {
        gsap.registerPlugin(ScrollTrigger);
    }

    if (hero) {
        animateHomeHero(hero);
    }

    const pendingCarousel = document.querySelector("#carrousel-lista-ocupaciones-pendientes");
    if (pendingCarousel) {
        animatePendingCarouselContainer(pendingCarousel);
        animatePendingCarouselChrome(pendingCarousel);
        animatePendingCards(pendingCards, false, true);

        initializeGsapPendingCarousel(pendingCarousel);
    } else {
        animatePendingCards(pendingCards);
    }
}

function animateHomeHero(hero) {
    const photo = hero.querySelector(".js-gsap-hero-image");
    const brand = hero.querySelector(".js-gsap-hero-logo");
    const lines = hero.querySelectorAll(".js-gsap-hero-line");
    const rail = hero.querySelector(".js-gsap-hero-rail");

    const timeline = gsap.timeline({ defaults: { ease: "expo.out" } });

    if (photo) {
        timeline.fromTo(photo,
            { scale: 1.08, filter: "blur(8px) saturate(1.15) contrast(1.05) brightness(0.8)" },
            { scale: 1, filter: "blur(0px) saturate(1.15) contrast(1.05) brightness(0.8)", duration: 1.8, clearProps: "transform" },
            0);
    }
    if (brand) {
        timeline.from(brand, { autoAlpha: 0, y: -8, duration: 0.9, clearProps: "opacity,visibility,transform" }, 0.1);
    }
    if (lines.length) {
        timeline.from(lines, { yPercent: 105, duration: 1.1, stagger: 0.09, clearProps: "transform" }, 0.15);
    }
    if (rail) {
        timeline.from(rail, { autoAlpha: 0, y: 10, duration: 0.8, clearProps: "opacity,visibility,transform" }, 0.75);
    }
}

function animatePendingCarouselContainer(carousel) {
    if (!window.gsap || !carousel || carousel.dataset.gsapContainerAnimated === "true") {
        return;
    }

    carousel.dataset.gsapContainerAnimated = "true";

    gsap.fromTo(carousel,
        {
            autoAlpha: 0,
            y: 24,
            scale: 0.985,
            transformOrigin: "50% 50%"
        },
        {
            autoAlpha: 1,
            y: 0,
            scale: 1,
            duration: 0.82,
            ease: "power2.out",
            overwrite: "auto",
            clearProps: "opacity,visibility,transform",
            scrollTrigger: window.ScrollTrigger ? {
                trigger: carousel,
                start: "top 88%",
                once: true
            } : undefined
        }
    );
}

function initializeGsapPendingCarousel(carousel) {
    if (!window.gsap || !window.bootstrap || !window.bootstrap.Carousel || !carousel) {
        return;
    }

    const carouselInner = carousel.querySelector(".carousel-inner");
    if (!carouselInner) {
        return;
    }

    const slides = Array.from(carousel.querySelectorAll(".carousel-item"));
    if (slides.length < 2) {
        return;
    }

    carousel.classList.add("carousel-gsap-enabled");

    const carouselInstance = window.bootstrap.Carousel.getOrCreateInstance(carousel);
    let isAnimating = false;
    let queuedDirection = null;

    carousel.addEventListener("slide.bs.carousel", function (event) {
        event.preventDefault();

        const outgoingSlide = carousel.querySelector(".carousel-item.active") || slides[0];
        const incomingSlide = getIncomingSlideFromEvent(event, slides, outgoingSlide);

        if (!outgoingSlide || !incomingSlide || outgoingSlide === incomingSlide) {
            return;
        }

        const direction = getCarouselDirection(event, slides, outgoingSlide, incomingSlide);

        if (isAnimating) {
            queuedDirection = direction;
            return;
        }

        isAnimating = true;
        const incomingCards = Array.from(incomingSlide.querySelectorAll(".js-gsap-pending-card"));
        const outgoingCards = Array.from(outgoingSlide.querySelectorAll(".js-gsap-pending-card"));

        const targetInnerHeight = Math.max(outgoingSlide.offsetHeight, incomingSlide.offsetHeight);
        if (targetInnerHeight) {
            gsap.set(carouselInner, { minHeight: targetInnerHeight + "px" });
        }

        gsap.killTweensOf([outgoingSlide, incomingSlide]);
        gsap.killTweensOf(outgoingCards);
        gsap.killTweensOf(incomingCards);

        gsap.set(carouselInner, { position: "relative", overflow: "hidden" });
        gsap.set([outgoingSlide, incomingSlide], {
            position: "absolute",
            inset: 0,
            width: "100%",
            display: "block"
        });

        gsap.set(outgoingSlide, { xPercent: 0, autoAlpha: 1, zIndex: 2 });
        gsap.set(incomingSlide, {
            xPercent: direction === "next" ? 12 : -12,
            autoAlpha: 0,
            zIndex: 3
        });

        if (outgoingCards.length) {
            gsap.set(outgoingCards, { x: 0, y: 0, scale: 1, autoAlpha: 1 });
        }

        if (incomingCards.length) {
            gsap.set(incomingCards, {
                x: direction === "next" ? 8 : -8,
                y: 6,
                scale: 0.992,
                autoAlpha: 0
            });
        }

        const timeline = gsap.timeline({
            defaults: { ease: "sine.out" },
            onComplete: function () {
                outgoingSlide.classList.remove("active");
                incomingSlide.classList.add("active");

                gsap.set([outgoingSlide, incomingSlide], {
                    clearProps: "position,inset,width,display,xPercent,zIndex"
                });
                gsap.set(outgoingSlide, { clearProps: "opacity,visibility" });
                gsap.set(incomingSlide, { clearProps: "opacity,visibility" });
                gsap.set(carouselInner, { clearProps: "minHeight" });
                if (outgoingCards.length) {
                    gsap.set(outgoingCards, { clearProps: "x,y,scale,opacity,visibility" });
                }
                if (incomingCards.length) {
                    gsap.set(incomingCards, { clearProps: "x,y,scale,opacity,visibility" });
                }

                animatePendingCards(incomingCards, true);

                isAnimating = false;
                if (queuedDirection) {
                    const nextDirection = queuedDirection;
                    queuedDirection = null;
                    if (nextDirection === "prev") {
                        carouselInstance.prev();
                        return;
                    }

                    carouselInstance.next();
                }
            }
        });

        timeline
            .to(outgoingSlide, {
                autoAlpha: 0.08,
                xPercent: direction === "next" ? -9 : 9,
                duration: 0.72,
                ease: "sine.inOut",
                overwrite: true
            }, 0)
            .fromTo(incomingSlide,
                {
                    autoAlpha: 0,
                    xPercent: direction === "next" ? 12 : -12
                },
                {
                    autoAlpha: 1,
                    xPercent: 0,
                    duration: 0.9,
                    ease: "power2.out",
                    overwrite: true
                },
                0.08
            );

        if (outgoingCards.length) {
            timeline.to(outgoingCards, {
                autoAlpha: 0.22,
                x: direction === "next" ? -5 : 5,
                y: -4,
                scale: 0.988,
                duration: 0.62,
                ease: "sine.inOut",
                stagger: 0.025,
                overwrite: true
            }, 0);
        }

        if (incomingCards.length) {
            timeline.to(incomingCards, {
                autoAlpha: 1,
                x: 0,
                y: 0,
                scale: 1,
                duration: 0.78,
                ease: "power2.out",
                stagger: 0.03,
                overwrite: true
            }, 0.16);
        }

    });
}

function getIncomingSlideFromEvent(event, slides, fallbackSlide) {
    if (event.relatedTarget) {
        return event.relatedTarget;
    }

    if (typeof event.to === "number" && slides[event.to]) {
        return slides[event.to];
    }

    return fallbackSlide;
}

function getCarouselDirection(event, slides, outgoingSlide, incomingSlide) {
    if (event.direction === "left") {
        return "next";
    }

    if (event.direction === "right") {
        return "prev";
    }

    const fromIndex = typeof event.from === "number" ? event.from : slides.indexOf(outgoingSlide);
    const toIndex = typeof event.to === "number" ? event.to : slides.indexOf(incomingSlide);

    if (fromIndex === -1 || toIndex === -1 || fromIndex === toIndex) {
        return "next";
    }

    if (fromIndex === slides.length - 1 && toIndex === 0) {
        return "next";
    }

    if (fromIndex === 0 && toIndex === slides.length - 1) {
        return "prev";
    }

    return toIndex > fromIndex ? "next" : "prev";
}

function animatePendingCarouselChrome(carousel) {
    if (!window.gsap || !carousel) {
        return;
    }

    const controls = carousel.querySelectorAll(".carousel-control-prev, .carousel-control-next");

    if (controls.length) {
        gsap.fromTo(controls,
            {
                autoAlpha: 0,
                scale: 0.92
            },
            {
                autoAlpha: 1,
                scale: 1,
                duration: 0.7,
                stagger: 0.08,
                ease: "power2.out",
                scrollTrigger: window.ScrollTrigger ? {
                    trigger: carousel,
                    start: "top 88%"
                } : undefined
            }
        );
    }
}

function animatePendingCards(cards, force, isInitialRender) {
    if (!window.gsap || !cards || !cards.length) {
        return;
    }

    const cardsToAnimate = force
        ? cards
        : cards.filter(function (card) {
            return card.closest(".carousel-item") === null || card.closest(".carousel-item").classList.contains("active");
        });

    const shouldAnimateRightToLeft = Boolean(isInitialRender && !force);
    const animationOrder = shouldAnimateRightToLeft
        ? cardsToAnimate.slice().reverse()
        : cardsToAnimate;

    if (!cardsToAnimate.length) {
        return;
    }

    cardsToAnimate.forEach(function (card) {
        const header = card.querySelector(".ocp-card__header");
        const body = card.querySelector(".ocp-card__body");
        const footer = card.querySelector(".ocp-card__footer");

        gsap.set([header, body, footer].filter(Boolean), { clearProps: "all" });
    });

    gsap.fromTo(animationOrder,
        {
            autoAlpha: 0,
            y: 26,
            scale: 0.975,
            rotateX: force ? 0 : 3,
            transformOrigin: "50% 100%"
        },
        {
            autoAlpha: 1,
            y: 0,
            scale: 1,
            rotateX: 0,
            duration: force ? 0.62 : 0.72,
            stagger: 0.1,
            ease: "power3.out",
            overwrite: true,
            scrollTrigger: !force && window.ScrollTrigger ? {
                trigger: cardsToAnimate[0],
                start: "top 88%"
            } : undefined
        }
    );

    animationOrder.forEach(function (card, index) {
        const header = card.querySelector(".ocp-card__header");
        const body = card.querySelector(".ocp-card__body");
        const footer = card.querySelector(".ocp-card__footer");
        const timelineDelay = index * 0.08 + (force ? 0.06 : 0.14);

        if (!header && !body && !footer) {
            return;
        }

        gsap.timeline({ defaults: { ease: "sine.out" }, delay: timelineDelay })
            .from(header, { autoAlpha: 0, y: 10, duration: 0.28 }, 0)
            .from(body, { autoAlpha: 0, y: 12, duration: 0.34 }, 0.06)
            .from(footer, { autoAlpha: 0, y: 8, duration: 0.24 }, 0.14);
    });
}
