import CoreImage
import Metal
import OpenPocketViewCore
import XCTest
@testable import OpenPocketCine

/// LUT / peaking / false colour / zebra share `needsGPUFeed` → `FeedFrameBaker` →
/// texel-for-texel blit. A leftover `scaleY: -1` anywhere in that chain inverted
/// all four versus the clean HEVC layer. `LiveFeedPresent` is gone — the present
/// is now a plain blit — so the testable seam is the baker's texture orientation.
final class LiveFeedOrientationTests: XCTestCase {
    func testGPUAssistsShareOneFeedFlag() {
        XCTAssertTrue(Self.peaking.needsGPUFeed)
        XCTAssertTrue(Self.zebra.needsGPUFeed)
        XCTAssertTrue(Self.falseColor.needsGPUFeed)
        XCTAssertTrue(Self.lut.needsGPUFeed)
        XCTAssertFalse(LiveImageEffects().needsGPUFeed)
        XCTAssertFalse(LiveImageEffects().needsSample)
        var face = LiveImageEffects()
        face.faceAF = true
        XCTAssertTrue(face.needsSample)
        XCTAssertFalse(face.needsGPUFeed)
        XCTAssertTrue(Self.peaking.needsOverlayFeed)
        XCTAssertTrue(Self.zebra.needsOverlayFeed)
        XCTAssertTrue(Self.falseColor.needsOverlayFeed)
        XCTAssertFalse(Self.lut.needsOverlayFeed)
        XCTAssertFalse(Self.falseColor.replacesIdentityFeed)
        XCTAssertTrue(Self.lut.replacesIdentityFeed)
    }

    func testCompositorKeepsVerticalMarkerForEveryGPUAssist() {
        let source = Self.verticalMarker()
        XCTAssertTrue(Self.topBandIsBrighter(source))
        for (name, fx) in [
            ("peaking", Self.peaking),
            ("zebra", Self.zebra),
            ("false color", Self.falseColor),
            ("LUT", Self.lut),
        ] {
            let output = LiveMonitorCompositor.apply(to: source, effects: fx)
            XCTAssertEqual(output.extent, source.extent, "\(name) must not change extent")
            XCTAssertTrue(
                Self.topBandIsBrighter(output),
                "\(name) must not invert the raster — colour science stays, orientation stays")
        }
        for (name, fx) in [
            ("peaking overlay", Self.peaking),
            ("zebra overlay", Self.zebra),
            ("false color overlay", Self.falseColor),
        ] {
            let overlay = LiveMonitorCompositor.assistOverlay(from: source, effects: fx)
            XCTAssertEqual(overlay.extent, source.extent, "\(name) must not change extent")
        }
    }

    func testBakerRoundTripKeepsVerticalMarker() throws {
        guard let device = MTLCreateSystemDefaultDevice() else {
            throw XCTSkip("Metal required")
        }
        let baker = FeedFrameBaker(device: device)
        let source = Self.verticalMarker()
        let drawable = CGSize(width: 64, height: 64)
        let done = expectation(description: "bake")
        baker.scheduleBake(
            image: source, drawableSize: drawable, pixelFormat: .bgra8Unorm
        ) {
            done.fulfill()
        }
        wait(for: [done], timeout: 2)
        guard let texture = baker.bakedTexture(for: drawable, pixelFormat: .bgra8Unorm) else {
            XCTFail("baker must publish a texture")
            return
        }
        defer { baker.releaseBakedTexture(texture) }
        // The bake is in drawable orientation; CIImage(mtlTexture:) re-flips, so
        // the wrapped image must read upright with no extra transform — exactly
        // the pair of cancelling flips the blit present relies on.
        guard
            let wrapped = CIImage(
                mtlTexture: texture,
                options: [.colorSpace: CGColorSpaceCreateDeviceRGB()])
        else {
            XCTFail("CIImage(mtlTexture:) must wrap the bake")
            return
        }
        XCTAssertTrue(
            Self.topBandIsBrighter(wrapped),
            "bake must land in drawable orientation — a leftover flip inverts the feed")
        XCTAssertFalse(
            Self.topBandIsBrighter(Self.leftoverVerticalFlip(wrapped)),
            "re-applying the old scaleY: -1 is the inversion the operator saw")
    }

    private static var peaking: LiveImageEffects {
        var fx = LiveImageEffects()
        fx.peaking = true
        return fx
    }

    private static var zebra: LiveImageEffects {
        var fx = LiveImageEffects()
        fx.zebra = true
        fx.zebraHighlight = true
        fx.zebraMidtone = false
        return fx
    }

    private static var falseColor: LiveImageEffects {
        var fx = LiveImageEffects()
        fx.falseColor = true
        fx.falseColorScale = .ire
        return fx
    }

    private static var lut: LiveImageEffects {
        let cube = BuiltInLook.mono.cube()
        var fx = LiveImageEffects()
        fx.lutDimension = cube.size
        fx.lutRGBA = cube.rgbaComponents.withUnsafeBytes { Data($0) }
        return fx
    }

    /// White band on the CI top (high y), black below — a vertical flip moves the energy down.
    private static func verticalMarker(width: CGFloat = 32, height: CGFloat = 32) -> CIImage {
        let black = CIImage(color: CIColor(red: 0, green: 0, blue: 0))
            .cropped(to: CGRect(x: 0, y: 0, width: width, height: height))
        let band = CIImage(color: CIColor(red: 1, green: 1, blue: 1))
            .cropped(to: CGRect(x: 0, y: height * 0.75, width: width, height: height * 0.25))
        return band.composited(over: black)
    }

    private static func leftoverVerticalFlip(_ image: CIImage) -> CIImage {
        image
            .transformed(by: CGAffineTransform(scaleX: 1, y: -1))
            .transformed(by: CGAffineTransform(translationX: 0, y: image.extent.height))
    }

    private static func topBandIsBrighter(_ image: CIImage) -> Bool {
        let context = CIContext(options: [.cacheIntermediates: false])
        let e = image.extent
        guard e.width > 2, e.height > 2 else { return false }
        let top = meanLuma(
            image,
            rect: CGRect(x: e.minX, y: e.midY, width: e.width, height: e.height / 2),
            context: context)
        let bottom = meanLuma(
            image,
            rect: CGRect(x: e.minX, y: e.minY, width: e.width, height: e.height / 2),
            context: context)
        return top > bottom + 0.08
    }

    private static func meanLuma(_ image: CIImage, rect: CGRect, context: CIContext) -> Float {
        let w = max(1, Int(rect.width.rounded(.down)))
        let h = max(1, Int(rect.height.rounded(.down)))
        var bytes = [UInt8](repeating: 0, count: w * h * 4)
        context.render(
            image, toBitmap: &bytes, rowBytes: w * 4,
            bounds: CGRect(x: rect.minX, y: rect.minY, width: CGFloat(w), height: CGFloat(h)),
            format: .RGBA8, colorSpace: CGColorSpaceCreateDeviceRGB())
        var sum: Float = 0
        let pixels = w * h
        for i in 0..<pixels {
            let r = Float(bytes[i * 4])
            let g = Float(bytes[i * 4 + 1])
            let b = Float(bytes[i * 4 + 2])
            sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
        }
        return sum / Float(max(pixels, 1)) / 255
    }
}
