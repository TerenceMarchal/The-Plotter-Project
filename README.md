# The Plotter Project
## Introduction

This project aims to create real-life drawings from pictures using a CNC Plotter machine.

It consists of a software that takes any bitmap image as input, and output drawing instructions.

The software handles two steps that are independent:
1. Convert an image into drawing instructions
2. Stream the drawing instructions to a drawing machine

You can watch a short introduction here: https://youtu.be/2gvqSbXmRnE

## Features
### Image processing
- Color quantization
    - RGB, sRGB and human-weighted color spaces
    - use only available inks colors, favor contrasts or color trues to the original
- Re-colorization
    - assign closest color, closest available ink color or favor contrasts
- Outlining
    - fine outlining: single line outline
    - thick outlining: apply the path generation on "thick" outlines detected by the Canny algorithm
- Brush palette & path generation
    - fill the recolored area with lines more or less close, with various angles and color
- Path optimization through genetic algorithm
    - reduce the non-drawing flying movements of the Plotter
    - very precise drawing duration estimation
    - G-Code instructions generation

### Instruction streaming
- Manual control
- Job previewing & streaming
- Clean & emergency pause, job resume and abortion
- Virtual Plotter for test & simulation
- Real-time visualization and remaining time estimation
- Automatic pen selection (tool change)


## Compatibility

This software is compatible with both Cartesian and CoreXY CNC.
Even though this software has been designed to be compatible with different CNC machine and instructions set, it is currently only compatible with G-Code instructions. The manual control features are to this day only compatible with GRBL v1.1.

When an image is converted into drawing instructions, multiples G-Code files are generated:
- one G-Code file per drawing color
- one G-Code file, that contains all the other G-Code files as well as specific tool-change instructions so that the Plotter can select the right color automatically

The single-color G-Code files can be streamed (or transferred) to any G-Code compatible Plotter with any standard G-Code streamer software (such as Universal GGcode Sender for instance). However, as the tool-change process is still heavily machine-specific, it is best to use the streaming feature included in this software to send the single G-Code file containing the instructions for all the colors to the Plotter.


## To-Do List
### Notice
I have been working on this project alone, on my free time. I'm taking a year off between August 2024 and August 2025, and won't be available to work on these features in the meantime.

### Release v1.0.0
#### Clean-up
- [x] 002 clean and move transformations-related algorithms
- [x] 003 move the image-processing GUI classes in a specific package
- [x] 004 replace time by duration and length by distance everywhere needed
- [x] 040 choose a licence
- [x] 041 add doc on streaming package
- [x] 055 complete the README presentation


#### Functionalities
- [x] 001 add prefixing zeros to clock-style duration string beautifier
- [x] 005 check if the brighter color are good enough for the flying motion visualization
- [x] 006 set the ink colors and pen tip width from the scanned drawings
- [x] 007 sort the colors in the ColoredProgressBar
- [x] 008 fix the Views zooming and panning issues
- [x] 009 disable the Streaming view pop-up menu until it is fully implemented
- [x] 042 check that the Plotter speed and accelerations are correct, set the tool positions and reachable and drawing area dimensions
- [x] 043 let the user simulate streaming through a Virtual Plotter
- [x] 045 add tooltips on settings to explain them
- [x] 010 use icons for buttons
- [x] 018 handle CoreXY design for the Path duration computation
- [x] 026 handle the G-Code feed parameter
- [x] 016 handle missing input file configuration
- [x] 025 save configuration as a .json file instead of using Java serialization
- [x] 021 read the Plotter settings from a configuration file or from the machine itself
- [x] 047 make the available inks color configurable
- [x] 015 better handle configuration modifications and computation abortion
- [x] 046 implement presets configuration, put the current settings into an advanced panel
- [x] 017 handle the Job pausing and abortion


### Release v1.1.0
#### Clean-up
Nothing yet

#### Functionalities
- [ ] 028 re-open the last one from the image-editing and the streaming panels on startup
- [ ] 030 do something more user-friendly for the previewing Job translation
- [ ] 052 handle import/export configuration

- [ ] 012 compare the computed expected durations and real-life measurements, improve the computations
- [ ] 013 implement the G-Code macros
- [ ] 014 try to merge TransformationResultChangeListener and ComputationProgressionListener
- [ ] 044 save the drawn Jobs from previous sessions
- [ ] 056 handle clean Job pause


### For further releases
#### Clean-up
Nothing yet


#### Functionalities
- [ ] 023 improve G-Code files parsing and generation, use a inherited class per Instruction type
- [ ] 019 implement the Streaming view popup menu functionalities

- [ ] 020 improve the handling of the initial tool for Job
- [ ] 011 clean the palette generation algorithms
- [ ] 022 finish implementing thinning algorithm
- [ ] 024 improve the Path optimization algorithms
- [ ] 048 Clean and improve the small path segments removal
- [ ] 027 handle G-Code files with relatives motions and inches units
- [ ] 051 use the right color space when selecting a median color
- [ ] 053 clean and refactor the Configuration class


### Nice to have but not urgent at all
- [ ] 029 clean the mirroring transformation
- [ ] 031 improve color sorting
- [ ] 032 improve the Job Instructions list handling
- [ ] 033 optimize fine outlining path generation by recognizing more pattern and not only checking the adjacent pixels
- [ ] 034 choose between tracking progression by the drawing progression or the estimated duration
- [ ] 035 optimize the N choose K combination function
- [ ] 036 improve the BrushEditor class
- [ ] 037 add more settings in the BrushEditor?
- [ ] 038 check and clean the singleton usage
- [ ] 039 use a setting to define the path precision
- [ ] 050 improve the access to image pixels colors
- [ ] 049 clean and simplify the canny package
- [ ] 054 fix the "Cannot read field "width" because "this.componentInnards" is null" error
