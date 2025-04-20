# 📝 SubaControl - Development Notes

## 🚀 Current Development Status (Latest Update)

The SubaControl project has made significant progress with several new features and improvements. This document tracks the ongoing development status, fixes, and known issues.

### ✅ Recent Changes and Improvements

- **Enhanced UI**:

  - Implemented Material Design components for a more polished look
  - Added better visual feedback for user actions
  - Improved layout and usability

- **Permission Handling**:

  - Added better permission flow for overlay and accessibility services
  - Improved user guidance through permission request process
  - Clearer error messages when permissions are missing

- **Calibration System**:

  - Implemented a coordinate mapping calibration system
  - Added dialog UI for calibration process with top-left and bottom-right target points
  - Added persistence for calibration data using DataStore
  - Fixed cursor movement during calibration mode (movement events continue while only click events are handled specially)

- **Cursor Management**:

  - Added cursor restart functionality to recover from display issues
  - Implemented more resilient cursor positioning with screen bounds checking
  - Fixed error handling in cursor overlay position updates

- **WebSocket Connection**:

  - Improved connection reliability and error reporting
  - Added test server mode for debugging without active WebSocket connection
  - Enhanced error messages for connection issues

- **Code Organization**:
  - Better package structure with new `calibration` package
  - Improved event handling between components
  - Fixed build issues and addressed compiler warnings

### 🔄 Recent Fixes

1. Fixed WebSocket connection handling to better manage reconnection attempts
2. Resolved cursor movement skipping during calibration mode
3. Addressed build errors by properly importing missing functions (kotlin.math.max)
4. Fixed cursor overlay drawing and improved cursor appearance
5. Added proper error handling for view initialization failures
6. Fixed ambiguous division operators in coordinate mapping

### 🐞 Known Issues

1. **Tap Alignment**: Clicks still don't register at the exact position expected - needs further calibration adjustments
2. **Calibration Functionality**: While implemented, calibration doesn't fully solve the coordinate mapping issues yet
3. **WebSocket Stability**: Connection can sometimes drop and require manual app restart
4. **Performance**: Some latency observed during rapid cursor movements
5. **Gesture Support**: Still missing support for more complex gestures (pinch, swipe, scroll)

## 🔮 Next Development Focus

1. Fix tap alignment to ensure clicks register at the correct position
2. Improve calibration algorithm to better handle different screen ratios
3. Add more robust WebSocket reconnection handling
4. Implement smoother cursor animations and tracking
5. Add more extensive logging for debugging

## 📊 Development Progress

- **UI/UX**: 70% complete
- **Core Functionality**: 60% complete
- **Calibration System**: 40% complete
- **Stability**: 50% complete
- **Performance**: 60% complete

## 🛠️ Technical Debt Items

1. Need comprehensive error handling in WebSocket connection logic
2. Refactor cursor positioning code for better maintainability
3. Add unit tests for core functionality
4. Optimize event bus for better performance
5. Address memory usage in long-running cursor overlay service
