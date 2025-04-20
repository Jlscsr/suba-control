# 🖱️ SubaControl

**SubaControl** is a work-in-progress project that lets you control your Android phone using your laptop's mouse—like a dual screen setup, but with _vibes_. Imagine sliding your cursor from your laptop to your phone as if they were one. Yeah, we're trying to make that real.

---

## 📚 Table of Contents

- [🧠 Origin of the Idea](#-origin-of-the-idea-aka-why-am-i-like-this)
- [💡 Why "SubaControl"?](#-why-subacontrol-aka-whats-with-the-name)
- [⚙️ Tech Stack](#️-tech-stack)
- [✅ Current Features](#-current-features)
- [🐞 Known Issues / Quirks](#-known-issues--quirks)
- [👀 Notes](#-notes)
- [📌 TODO (Next Steps)](#-todo-next-steps)
- [👥 Contributors](#-contributors)

---

## 🧠 Origin of the Idea (a.k.a. "Why am I like this?")

So there I was—**too lazy to reach for my phone**, which was literally just chillin' beside me. All I wanted was to browse something real quick without moving my arm like it's the 1800s.

Then I thought:

> "Wouldn't it be dope if I could just drag my mouse _off_ my laptop screen… and it pops up on my phone like magic?"

I looked for apps that could do it—something like **Microsoft's Mouse Without Borders**, but for laptop-to-Android.  
But nah, all I found were glorified remote access tools (_cough_ Anydesk _cough_), or those clunky "mirror your phone to your PC" stuff. Bro. Why would I wanna control a copy of my phone on my laptop, when **the phone is literally right there**? 💀

So I had this thought:

> If **Mouse Without Borders** can treat two laptops as one extended screen…  
> Could I treat my **Android phone** as a secondary screen too?

**Boom. That's how SubaControl was born.** 🐣  
Not just another remote control thing. Not a clone app.  
**The goal is to simulate a real dual-screen behavior**, but using your phone + laptop combo. Like, vibe with the idea of sliding your cursor straight into your phone.

Still doing a lot of R&D and mouse sorcery, but hey—we move.  
This ain't about copying Microsoft.  
This is about **reinventing control** for our lazy future™. 😎

---

## 💡 Why "SubaControl"? (aka... what's with the name??)

So here's the lore™:

I'm a fan of **Oozora Subaru** from Hololive.  
One day while vibing and coding, I randomly thought:

> "Hmm... if I had to name this app after a duck that gives me serotonin, what would it be?"

And bam — **SubaControl**.

It's like "Subaru" + "Control,"  
aka **letting your inner duck take control of your devices**.

No deep tech explanation here. Just pure brainrot + fandom love = branding gold 😎🦆💻

---

## ⚙️ Tech Stack

- **Frontend (Android App)**: Kotlin, Jetpack Compose
- **Backend**: Node.js
- **Native Mouse Hook**: C++ (via `cmake-js`, `SetWindowsHookEx`)
- **Communication**: WebSocket
- **Event Bus**: Kotlin-based for UI interactions
- **Persistence**: DataStore for calibration settings

> ⚠️ **Disclaimer:** Super beginner pa ako sa Kotlin, so if it looks messy… you're right. 😂 I'm learning tho. Suggestions are very much welcome!

---

## ✅ Current Features

- 🔁 Mouse movement captured from laptop and sent to Android via WebSocket
- 🔍 Screen dimension scaling (laptop ↔ phone)
- 👆 Tap events sent from mouse clicks (touch simulation)
- 🧠 Native C++ hook for low-level mouse event capture
- 🧼 Modularized Kotlin Android app with clear package separation:
  - `overlay`, `websocket`, `accessibility`, `util`, `calibration`
- 🎯 Calibration system to adjust cursor mapping between devices
- 🔄 Cursor restart functionality for when things go wonky
- 🛡️ Improved permission handling for overlay and accessibility services
- 📱 Enhanced UI with Material Design components and better user feedback
- 🧪 Test server mode for debugging without an active WebSocket connection

---

## 🐞 Known Issues / Quirks

- 📍 Tap registration has a **position mismatch** — mouse click lands on the wrong UI element due to scaling/calibration issues
- 🧩 Calibration feature is implemented but not fully functional
- 🐢 Some latency spikes under unstable Wi-Fi
- 🫥 No gesture support _yet_ (pinch, swipe, scroll, etc.)
- 🧩 Native module needs further error handling and cleanup
- 🚨 WebSocket reconnection sometimes requires manual app restart
- 🔄 Cursor movement may skip during calibration in certain scenarios

---

## 👀 Notes

- 📦 This is a **read-only public repo**: no forks, no clones, no funny business (yet)
- 💡 **I'm open to suggestions and ideas**, even though the repo isn't open-source _for now_
- 🛠️ Planning to open-source this someday once it's less janky™
- 🧪 Expect breaking changes, chaotic commits, and temporary spaghetti (but trust, it's part of the vision)

---

## 📌 TODO (Next Steps)

- [ ] Fix calibration functionality to properly map coordinates
- [ ] Resolve tap alignment issues for more accurate touch simulation
- [ ] Improve cursor stability during calibration
- [ ] Add drag/gesture feedback on Android overlay
- [ ] Implement better WebSocket reconnection handling
- [ ] Optimize cursor movement for smoother experience
- [ ] Add multi-device support (future plan)
- [ ] Clean up native C++ module memory handling
- [ ] Add unit tests for core functionality

---

## 👥 Contributors

- 🧠 **Julius Caesar Raagas** — 60% Human Brainpower™
  - Ideation, backend logic, Kotlin setup (chaotic good), sleepless debugging
- 🤖 **ChatGPT** — 40% Silicon Gremlin
  - Refactoring buddy, README ghostwriter, and your friendly neighborhood code consultant

We co-parent this codebase. Please be nice to both of us. 🙏
