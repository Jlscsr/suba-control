feat(calibration): implement calibration system and fix cursor movement

This commit adds several major improvements to SubaControl:

- Add new calibration system with UI dialog
- Fix cursor movement during calibration mode
- Improve permission handling for overlay and accessibility
- Add cursor restart functionality
- Fix build errors with proper imports
- Update documentation with latest changes
- Enhance error handling in critical components

The calibration system now captures top-left and bottom-right coordinates
to properly map between desktop and phone screens. Cursor movement continues
during calibration, with only click events being specially handled.

Known issues:

- Calibration is implemented but not fully working for tap alignment
- Some WebSocket stability issues remain

Resolves: #12 (cursor movement during calibration)
Related: #8 (improved permission handling)
