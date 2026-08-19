import Foundation

/// H.264 / AVC NAL helpers for Osmo Nano live view. Same UDP 9004 / pktType `0x02`
/// / DJI marker as Pocket HEVC; parameter sets are AVC SPS/PPS (`0x67`/`0x68`),
/// not HEVC VPS/SPS/PPS. Captured 2026-08-18 (`mimo-nano-live-20260818`).
public enum Avc {
    /// AVC NAL unit type: `firstByte & 0x1f`.
    public static func nalType(_ firstByte: UInt8) -> Int { Int(firstByte & 0x1f) }

    public static let nonIdr = 1
    public static let idr = 5
    public static let sei = 6
    public static let sps = 7
    public static let pps = 8
    public static let aud = 9

    public static func isVCL(_ t: Int) -> Bool { (1...5).contains(t) }
    public static func isKeyframeNal(_ t: Int) -> Bool { t == sps || t == pps || t == idr }
}

/// Which live-view codec an Annex-B access unit is. Pocket is HEVC; Nano is AVC.
public enum LiveVideoCodec: Equatable, Sendable {
    case hevc
    case avc
}

public enum LiveVideo {
    /// Classify from a NAL's first byte. Parameter sets are unambiguous;
    /// P-slices alone are not classified.
    public static func codec(ofNAL firstByte: UInt8) -> LiveVideoCodec? {
        // HEVC param sets are 0x40/0x42/0x44. AVC P-slice 0x41 is HEVC type 32
        // (VPS) if we only look at `(b>>1)&0x3f` — that latched Nano P-frames as HEVC.
        switch firstByte {
        case 0x40, 0x42, 0x44: return .hevc
        default: break
        }
        let avc = Avc.nalType(firstByte)
        if avc == Avc.sps || avc == Avc.pps || avc == Avc.idr { return .avc }
        return nil
    }

    public static func detect(nals: [[UInt8]]) -> LiveVideoCodec? {
        for nal in nals {
            guard let first = nal.first, let codec = codec(ofNAL: first) else { continue }
            return codec
        }
        return nil
    }

    public static func detect(annexB: [UInt8]) -> LiveVideoCodec? {
        detect(nals: Hevc.nalUnits(annexB))
    }
}
