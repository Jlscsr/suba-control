# 🖱️ SubaControl

**SubaControl** is a work-in-progress project that lets you control your Android phone using your laptop’s mouse—like a dual screen setup, but with _vibes_. Imagine sliding your cursor from your laptop to your phone as if they were one. Yeah, we’re trying to make that real.

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

> “Wouldn't it be dope if I could just drag my mouse _off_ my laptop screen… and it pops up on my phone like magic?”

I looked for apps that could do it—something like **Microsoft's Mouse Without Borders**, but for laptop-to-Android.  
But nah, all I found were glorified remote access tools (_cough_ Anydesk _cough_), or those clunky "mirror your phone to your PC" stuff. Bro. Why would I wanna control a copy of my phone on my laptop, when **the phone is literally right there**? 💀

So I had this thought:

> If **Mouse Without Borders** can treat two laptops as one extended screen…  
> Could I treat my **Android phone** as a secondary screen too?

**Boom. That’s how SubaControl was born.** 🐣  
Not just another remote control thing. Not a clone app.  
**The goal is to simulate a real dual-screen behavior**, but using your phone + laptop combo. Like, vibe with the idea of sliding your cursor straight into your phone.

Still doing a lot of R&D and mouse sorcery, but hey—we move.  
This ain’t about copying Microsoft.  
This is about **reinventing control** for our lazy future™. 😎

---

## 💡 Why "SubaControl"? (aka... what’s with the name??)

So here's the lore™:

I'm a fan of **Oozora Subaru** from Hololive.  
One day while vibing and coding, I randomly thought:

> "Hmm... if I had to name this app after a duck that gives me serotonin, what would it be?"

And bam — **SubaControl**.

It’s like “Subaru” + “Control,”  
aka **letting your inner duck take control of your devices**.

No deep tech explanation here. Just pure brainrot + fandom love = branding gold 😎🦆💻

---

## ⚙️ Tech Stack

- **Frontend (Android App)**: Kotlin
- **Backend**: Node.js
- **Native Mouse Hook**: C++ (via `cmake-js`, `SetWindowsHookEx`)
- **Communication**: WebSocket
- **Event Bus**: Kotlin-based for UI interactions

> ⚠️ **Disclaimer:** Super beginner pa ako sa Kotlin, so if it looks messy… you’re right. 😂 I'm learning tho. Suggestions are very much welcome!

---

## ✅ Current Features

- 🔁 Mouse movement captured from laptop and sent to Android via WebSocket
- 🔍 Screen dimension scaling (laptop ↔ phone)
- 🧠 Native C++ hook for low-level mouse event capture
- 🧼 Modularized Kotlin Android app with clear package separation:
  - `overlay`, `websocket`, `accessibility`, `util`

---

## 🐞 Known Issues / Quirks

- 🖱️ Cursor positioning sometimes gets offset due to screen aspect ratio mismatch
- 🐢 Some latency spikes under unstable Wi-Fi
- 🫥 No touch interaction _yet_ (we’re cooking it)
- 🧩 Native module needs further error handling and cleanup
- 🤝 Accessibility service still rough around the edges

---

## 👀 Notes

- 📦 This is a **read-only public repo**: no forks, no clones, no funny business (yet)
- 💡 **I'm open to suggestions and ideas**, even though the repo isn’t open-source _for now_
- 🛠️ Planning to open-source this someday once it’s less janky™
- 🧪 Expect breaking changes, chaotic commits, and temporary spaghetti (but trust, it’s part of the vision)

---

## 📌 TODO (Next Steps)

- [ ] Improve input precision & scaling
- [ ] Add tap/gesture feedback on Android overlay
- [ ] Bi-directional communication (future plan)
- [ ] Clean up native C++ module memory handling
- [ ] Deploy WebSocket connection as a background service

---

## 👥 Contributors

- 🧠 **Julius Caesar Raagas** — 60% Human Brainpower™
  - Ideation, backend logic, Kotlin setup (chaotic good), sleepless debugging
- 🤖 **ChatGPT** — 40% Silicon Gremlin
  - Refactoring buddy, README ghostwriter, and your friendly neighborhood code consultant

We co-parent this codebase. Please be nice to both of us. 🙏
