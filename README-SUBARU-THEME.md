# SubaControl - Oozora Subaru Theme

This document details the implementation of the Oozora Subaru navy-and-gold palette in the SubaControl app.

## Color Palette

The following color palette has been implemented:

| Token                 | Hex               | Usage                         |
| --------------------- | ----------------- | ----------------------------- |
| primary               | #1F3057           | Navy jacket                   |
| primaryContainer      | #29447A           | Lighter navy                  |
| secondary             | #F5D96B           | Gold buttons / star           |
| secondaryContainer    | #FFF3C1           | Pale gold                     |
| tertiary              | #3BC3F3           | Aqua ribbon accent            |
| error                 | #D03B3B           | Error red                     |
| background / surface  | #FFFFFF           | Screen white                  |
| onPrimary / onSurface | #FFFFFF / #1F3057 | Text colors based on contrast |

## Implementation Details

1. **SubaColors.kt** - Contains color constants for the Oozora Subaru palette
2. **Theme.kt** - Provides `SubaControlTheme` composable using Material 3 design system
3. **MainActivity.kt** - Updated to use the new theme
4. **CursorOverlay.kt** - Updated cursor tint to use Subaru Navy with 80% opacity
5. **ControlPanelScreen.kt** - Updated to use Material 3 components and new color scheme
6. **CalibrationDialog.kt** - Updated to use Material 3 components and new color scheme

## Changes Made

- Created color constants using the provided Subaru palette
- Implemented a Material 3 theme using lightColorScheme with appropriate mappings
- Added dark theme stub for future implementation
- Updated all UI components to Material 3
- Applied correct color contrast for text on different backgrounds
- Changed cursor overlay tint to navy blue (80% opacity)
- Updated calibration target to gold color

## QA Checklist

- [ ] App launches with new navy and gold color scheme
- [ ] Cursor overlay is visible on both white and dark surfaces (navy tint)
- [ ] All buttons use navy background with white text
- [ ] Secondary components (loading spinner, status indicators) use gold
- [ ] Text has appropriate contrast (navy on white, white on navy)
- [ ] Cursor moves and clicks properly with new tint
- [ ] Calibration screen shows gold target circles

## Notes

- The calibration dialog now uses gold target circles instead of red for better brand consistency
- The app theme follows Material 3 guidelines for better future compatibility
- Cursor overlay uses semi-transparent navy for better visibility on all backgrounds
- Status bar is now navy colored to match the app's theme
