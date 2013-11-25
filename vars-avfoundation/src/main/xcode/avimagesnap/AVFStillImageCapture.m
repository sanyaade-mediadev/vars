//
//  AVFStillImageCapture.m
//  avimagesnap
//
// https://developer.apple.com/library/ios/qa/qa1702/_index.html
// http://www.benjaminloulier.com/posts/ios4-and-direct-access-to-the-camera/
// http://stackoverflow.com/questions/1320988/saving-cgimageref-to-a-png-file
//
//  Created by Brian Schlining on 11/21/13.
//  Copyright (c) 2013 Brian Schlining. All rights reserved.
//

#import "AVFStillImageCapture.h"

@implementation AVFStillImageCapture

@synthesize session;
@synthesize videoInput;
@synthesize stillImageOutput;


+(NSArray *) videoCaptureDevices {
    NSArray *devices = [AVCaptureDevice devices];
    NSMutableArray *videoDevices = [NSMutableArray array];
    for (AVCaptureDevice *device in devices) {
        if ([device hasMediaType:AVMediaTypeVideo]) {
            [videoDevices addObject:device];
        }
    }
    return videoDevices;
};

+(NSArray *) videoCaptureDevicesAsStrings {
    NSArray *videoDevices = [self videoCaptureDevices];
    NSMutableArray *localizedNames = [NSMutableArray array];
    for (AVCaptureDevice *device in videoDevices) {
        [localizedNames addObject:[device localizedName]];
    }
    return localizedNames;
};

+(AVCaptureDevice *) videoCaptureDeviceNamed: (NSString *)name {
    AVCaptureDevice *namedDevice = nil;
    NSArray *videoDevices = [self videoCaptureDevices];
    for (AVCaptureDevice *device in videoDevices) {
        if ([[device localizedName] isEqualToString:name]) {
            namedDevice = device;
        }
    }
    return namedDevice;
};

-(void)  setupCaptureSessionUsingNamedDevice: (NSString *) name {
    
    if (session == nil) {
        session = [[AVCaptureSession alloc] init];
        session.sessionPreset = AVCaptureSessionPresetPhoto;
        [session startRunning];
    }
    
    if (session != nil) {
        
        [session beginConfiguration];

        if (videoInput != nil) {
            [session removeInput:videoInput];
            videoInput = nil;
        }
        
        if (stillImageOutput != nil) {
            [session removeOutput:stillImageOutput];
            stillImageOutput = nil;
        }
        
        AVCaptureDevice *device = [AVFStillImageCapture videoCaptureDeviceNamed:name];
        if (device != nil) {
            NSError *error = nil;
            videoInput = [AVCaptureDeviceInput deviceInputWithDevice:device error:&error];
            if (videoInput != nil) {
                [session addInput:videoInput];
                
                // create and configure output
                stillImageOutput = [[AVCaptureStillImageOutput alloc] init];
                [session addOutput:stillImageOutput];
            }
            else {
                NSLog(@"Unable to create a video input from the device named '%@'", name);
            }
            
        }
        else {
            NSLog(@"Unable to open video device named '%@", name);
        }
        
        [session commitConfiguration];
        
        // First framegrab on DeckLink is always black. So just grab one and don't do anything with it.
        AVCaptureConnection *captureConnection = nil;
        if (stillImageOutput != nil) {
            captureConnection = [[stillImageOutput connections] objectAtIndex:0];
            [stillImageOutput captureStillImageAsynchronouslyFromConnection:captureConnection completionHandler:^(CMSampleBufferRef imageDataSampleBuffer, NSError *error) {
                
                [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageDataSampleBuffer];
            }];
        }
            
    }
}

/*-(void) saveStillImageToPath: (NSString *) path {
    
    AVCaptureConnection *captureConnection = nil;
    
    if (stillImageOutput != nil) {
        captureConnection = [[stillImageOutput connections] objectAtIndex:0];
    }
    
    if (captureConnection != nil) {
        
        [stillImageOutput captureStillImageAsynchronouslyFromConnection:captureConnection completionHandler:^(CMSampleBufferRef imageDataSampleBuffer, NSError *error) {
            
            // --- Convert to a CGImageRef
            // This FAILS and imageBuffer is NULL ... no idea why as it's straight from ALL of 
            // Apples' samples.
            CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(imageDataSampleBuffer);
            
            // Lock imageBuffer
            CVPixelBufferLockBaseAddress(imageBuffer, 0);
            
            // Get info about image
            uint8_t *baseAddress = (uint8_t *) CVPixelBufferGetBaseAddress(imageBuffer);
            size_t bytesPerRow = CVPixelBufferGetBytesPerRow(imageBuffer);
            size_t width = CVPixelBufferGetWidth(imageBuffer);
            size_t height = CVPixelBufferGetHeight(imageBuffer);
            
            // Create a CGImageRef from CVImageBufferRef
            CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
            CGContextRef newContext = CGBitmapContextCreate(baseAddress, width, height, 8, bytesPerRow, colorSpace, kCGBitmapByteOrder32Little | kCGImageAlphaPremultipliedFirst);
            CGImageRef newImage = CGBitmapContextCreateImage(newContext);
            CGContextRelease(newContext);
            CGColorSpaceRelease(colorSpace);
            
            // --- Write as PNG
            CFURLRef url = (__bridge CFURLRef)[NSURL fileURLWithPath:path];
            CGImageDestinationRef destination = CGImageDestinationCreateWithURL(url, kUTTypePNG, 1, NULL);
            CGImageDestinationAddImage(destination, newImage, nil);
            if (!CGImageDestinationFinalize(destination)) {
                NSLog(@"Failed to write image to %@", path);
            }
            CFRelease(destination);
            CGImageRelease(newImage);
            
            // Unlock imageBuffer
            CVPixelBufferUnlockBaseAddress(imageBuffer, 0);
            
        }];

    }
} */

-(void) saveStillImageToPath:(NSString *)path {
    AVCaptureConnection *captureConnection = nil;
    
    if (stillImageOutput != nil) {
        captureConnection = [[stillImageOutput connections] objectAtIndex:0];
    }
    
    if (captureConnection != nil) {
        
        [stillImageOutput captureStillImageAsynchronouslyFromConnection:captureConnection completionHandler:^(CMSampleBufferRef imageDataSampleBuffer, NSError *error) {
            
            // Save as JPG
//            if (imageDataSampleBuffer != NULL) {
//                NSLog(@"Saving %@", path);
//                NSData *imageData = [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageDataSampleBuffer];
//                [imageData writeToFile:path atomically:YES];
//            }
            
            // Save as PNG
            if (imageDataSampleBuffer != NULL) {
                NSLog(@"Saving %@", path);
                NSData *jpgData = [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageDataSampleBuffer];
                NSBitmapImageRep *bitmap = [NSBitmapImageRep imageRepWithData:jpgData];
                NSData *pngData = [bitmap representationUsingType: NSPNGFileType properties:nil];
                [pngData writeToFile:path atomically:YES];
            }
        }];
    }
}

-(void) dealloc {
    [session stopRunning];
}

@end


