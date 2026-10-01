import SwiftUI

/// Native Canvas counterpart of Android LandEnvironmentDrawing: matching
/// palettes, landscape layers and bounded ambient motion, without bitmap assets.
struct LandEnvironmentView: View {
    let zone: ShellDepthTier
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 20, paused: reduceMotion)) { timeline in
            Canvas { context, size in
                LandLandscape(context: context, size: size, time: reduceMotion ? 0 : timeline.date.timeIntervalSinceReferenceDate)
                    .draw(zone)
            }
        }
        .accessibilityHidden(true)
        .allowsHitTesting(false)
    }
}

private struct LandLandscape {
    let context: GraphicsContext
    let size: CGSize
    let time: Double
    private var w: Double { size.width }
    private var h: Double { size.height }

    private func color(_ hex: UInt32, _ alpha: Double = 1) -> Color {
        Color(red: Double((hex >> 16) & 255) / 255, green: Double((hex >> 8) & 255) / 255, blue: Double(hex & 255) / 255).opacity(alpha)
    }
    private func point(_ x: Double, _ y: Double) -> CGPoint { CGPoint(x: w * x, y: h * y) }
    private func polygon(_ points: [(Double, Double)], _ hex: UInt32, _ alpha: Double = 1) {
        guard let first = points.first else { return }
        var path = Path(); path.move(to: point(first.0, first.1))
        for p in points.dropFirst() { path.addLine(to: point(p.0, p.1)) }
        path.closeSubpath(); context.fill(path, with: .color(color(hex, alpha)))
    }
    private func oval(_ x: Double, _ y: Double, _ width: Double, _ height: Double, _ hex: UInt32, _ alpha: Double = 1) {
        context.fill(Path(ellipseIn: CGRect(x: x*w, y: y*h, width: width*w, height: height*h)), with: .color(color(hex, alpha)))
    }
    private func circle(_ x: Double, _ y: Double, _ radius: Double, _ hex: UInt32, _ alpha: Double = 1) {
        oval(x-radius, y-radius*w/h, radius*2, radius*2*w/h, hex, alpha)
    }
    private func line(_ x1: Double, _ y1: Double, _ x2: Double, _ y2: Double, _ width: Double, _ hex: UInt32, _ alpha: Double = 1) {
        var path = Path(); path.move(to: point(x1,y1)); path.addLine(to: point(x2,y2))
        context.stroke(path, with: .color(color(hex,alpha)), style: StrokeStyle(lineWidth: w*width, lineCap: .round))
    }
    private func hill(_ hex: UInt32, _ y: Double, _ bend: Double, _ end: Double) {
        var path = Path(); path.move(to: point(0,y))
        path.addCurve(to: point(1,end), control1: point(0.25,y-bend), control2: point(0.66,end+bend))
        path.addLine(to: point(1,1)); path.addLine(to: point(0,1)); path.closeSubpath()
        context.fill(path, with: .color(color(hex)))
    }
    private func haze(_ hex: UInt32, _ y: Double, _ alpha: Double) {
        oval(-0.2+sin(time*0.045)*0.07,y,1.4,0.08,hex,alpha)
    }
    private func grass(_ x: Double, _ y: Double, _ scale: Double, _ hex: UInt32, _ alpha: Double, _ seed: Int) {
        let sway = sin(time*0.45+Double(seed))*scale*0.16
        for blade in 0..<3 {
            var path = Path(); path.move(to: point(x,y))
            path.addQuadCurve(to: point(x+(Double(blade)-1)*scale*0.44*h/w+sway*h/w,y-scale*(0.68+Double(blade)*0.15)),
                              control: point(x+(Double(blade)-1)*scale*0.25*h/w+sway*h/w,y-scale*0.5))
            context.stroke(path, with: .color(color(hex,alpha)), style: StrokeStyle(lineWidth: w*0.0018,lineCap: .round))
        }
    }
    private func stone(_ x: Double, _ y: Double, _ radius: Double, _ hex: UInt32) {
        let r = radius*w/h
        polygon([(x-radius,y),(x-radius*0.72,y-r*0.58),(x+radius*0.1,y-r*0.8),(x+radius*0.75,y-r*0.42),(x+radius,y)],hex)
    }
    private func gradient(_ colors: [UInt32]) {
        context.fill(Path(CGRect(origin: .zero,size: size)), with: .linearGradient(Gradient(colors: colors.map { color($0) }), startPoint: .zero, endPoint: CGPoint(x:0,y:h)))
    }

    func draw(_ zone: ShellDepthTier) {
        switch zone {
        case .goldenFields:
            gradient([0xF0DFC0,0xD9DAB3,0xADBA83])
            circle(0.75,0.24,0.14,0xF7E9BB,0.6); haze(0xF9EFDB,0.32,0.22)
            hill(0xC9CEA0,0.42,0.08,0.38); hill(0xB6C18C,0.54,0.065,0.46); hill(0xC4BC80,0.64,0.075,0.56)
            for row in 0..<7 {
                let y = 0.49+Double(row)*0.018
                var path = Path(); path.move(to: point(0,y))
                path.addCurve(to: point(0.53,y+0.065),control1: point(0.23,y-0.045),control2: point(0.40,y+0.006))
                context.stroke(path,with: .color(color(0xE7D695,0.38)),lineWidth: w*0.005)
            }
            hill(0xA9B580,0.74,0.05,0.70)
            for post in 0..<8 {
                let x = 0.03+Double(post)*0.075, y = 0.76-Double(post)*0.008
                line(x,y,x,y-0.025,0.004,0x8D936D,0.48)
                if post<7 { line(x,y-0.018,x+0.075,y-0.026,0.002,0x8D936D,0.35) }
            }
            for i in 0..<24 {
                let x = Double(i*37%101)/100, y = 0.65+Double(i*19%29)/100
                grass(x,y,0.014,0x72865D,0.45,i)
                if i%3 == 0 { circle(x,y-0.014,0.003,i%2 == 0 ? 0xDDCF9E : 0xD6BBA5) }
            }
        case .ancientWoods:
            gradient([0xB9C7B1,0x7E9A80,0x526F5D])
            for i in 0..<11 {
                let x = Double(i)/10, width = 0.018+Double(i%3)*0.007
                polygon([(x,0.12),(x+width,0.12),(x+width,0.74),(x,0.74)],0x5E7A65,0.23)
            }
            polygon([(0.42,0.18),(0.68,0.78),(0.98,0.78),(0.57,0.18)],0xE4E6BD,0.12)
            hill(0x71916E,0.61,0.07,0.57); hill(0x638264,0.77,0.06,0.72)
            for i in 0..<5 {
                let x = Double(i)*0.26-0.05, y = 0.69+Double(i%2)*0.07
                var path = Path(); path.move(to: point(x-0.025,y))
                path.addQuadCurve(to: point(x-0.035,0.14),control: point(x-0.01,0.36)); path.addLine(to: point(x+0.03,0.14))
                path.addQuadCurve(to: point(x+0.05,y),control: point(x+0.015,0.42)); path.closeSubpath()
                context.fill(path,with: .color(color(0x455F4D,0.52)))
                line(x,0.36,x+0.14,0.28,0.02,0x455F4D,0.45)
                for crown in 0..<3 { oval(x-0.16+Double(crown)*0.06,0.13+Double(crown)*0.04,0.35,0.2,0x476D51,0.3) }
                line(x,y,x-0.08,y+0.01,0.012,0x455F4D,0.3)
            }
            haze(0xD5DFBD,0.63,0.09)
            for i in 0..<17 {
                let x = Double(i*43%101)/100, y = 0.69+Double(i*17%25)/100
                grass(x,y,0.025,0x91A781,0.6,i)
                if i%4 == 0 { stone(x,y,0.022,0x82977B) }
            }
            for i in 0..<9 { circle(0.1+Double(i*29%80)/100,0.42+Double(i*13%30)/100+sin(time*0.22+Double(i))*0.003,0.0025,0xE1DCA3,(sin(time*0.55+Double(i)*1.7)+1)*0.12) }
        case .openSands:
            gradient([0xE5CFB0,0xE1CDA7,0xC2A276]); circle(0.72,0.26,0.115,0xF5DEAF,0.62)
            for i in 0..<3 {
                let x = 0.12+Double(i)*0.4, y = 0.43+Double(i%2)*0.025
                polygon([(x-0.13,y),(x-0.07,y-0.085),(x+0.06,y-0.085),(x+0.12,y)],0xAC947A,0.27)
            }
            hill(0xD5B78B,0.50,0.12,0.43); hill(0xE1C599,0.59,-0.09,0.55); hill(0xC9A97D,0.72,0.10,0.61); hill(0xD4B68B,0.80,-0.045,0.78)
            for i in 0..<8 {
                let y = 0.69+Double(i)*0.019
                var path = Path(); path.move(to: point(0.02,y)); path.addQuadCurve(to: point(0.40,y+0.025),control: point(0.24,y-0.018))
                context.stroke(path,with: .color(color(0xE8CCA0,0.36)),lineWidth: w*0.002)
            }
            for i in 0..<3 {
                let x = 0.12+Double(i)*0.36, y = 0.73+Double(i%2)*0.08
                line(x,y,x,y-0.055,0.012,0x7F8B6C,0.75); line(x,y-0.023,x-0.025,y-0.023,0.009,0x7F8B6C,0.75)
                line(x-0.025,y-0.023,x-0.025,y-0.043,0.009,0x7F8B6C,0.75); grass(x+0.045,y,0.015,0x9F946D,1,i)
            }
            for i in 0..<12 {
                let x = (Double(i)*0.083+time*0.002).truncatingRemainder(dividingBy: 1), y = 0.53+Double(i*17%35)/100
                line(x,y,x+0.025,y-0.003,0.0015,0xF2DDB8,0.23)
            }
        case .highPeaks:
            gradient([0xD6E1E3,0xB7CBCB,0x8EABA8])
            for i in 0..<5 {
                let x = Double(i)*0.29-0.08, top = 0.31+Double(i%3)*0.045, base = 0.64+Double(i%2)*0.04
                polygon([(x-0.32,base),(x-0.065,top+0.075),(x,top),(x+0.08,top+0.09),(x+0.32,base)],0x7D969E,0.52)
                polygon([(x-0.11,top+0.12),(x,top),(x+0.13,top+0.145),(x+0.045,top+0.1),(x,top+0.115),(x-0.05,top+0.075)],0xE8EEEA,0.8)
                polygon([(x,top),(x+0.32,base),(x+0.04,base)],0x5D7D89,0.17)
            }
            haze(0xE5EDE8,0.54,0.25); hill(0x99B2AC,0.66,0.025,0.65); hill(0x849F98,0.80,0.07,0.74)
            for i in 0..<8 {
                let x = Double(i)*0.15-0.03, y = 0.77+Double(i%2)*0.06, height = 0.07+Double(i%3)*0.014
                line(x,y,x,y-height,0.005,0x526F69,0.6)
                for tier in 0..<3 {
                    let top = y-height+height*Double(tier)*0.21, spread = height*(0.15+Double(tier)*0.055)*h/w
                    polygon([(x,top),(x+spread,top+height*0.48),(x-spread,top+height*0.48)],0x526F69,0.6)
                }
                let sx = Double(i*41%100)/100, sy = 0.72+Double(i%3)*0.07
                stone(sx,sy,0.023+Double(i%2)*0.012,0x718B86); oval(sx-0.025,sy-0.013,0.05,0.007,0xE0E6DA,0.6)
            }
            for i in 0..<15 { circle((Double(i)*0.073+sin(time*0.12+Double(i))*0.012).truncatingRemainder(dividingBy: 1),(Double(i)*0.061+time*0.0025).truncatingRemainder(dividingBy: 1),0.0018,0xF4F4E9,0.32) }
        default:
            gradient([0xBFC0AF,0x8A9E8D,0x4E6C5D]); circle(0.72,0.28,0.13,0xE2D3A9,0.42)
            hill(0x7E9380,0.45,0.09,0.39); hill(0x698571,0.59,0.035,0.53); hill(0x5B7964,0.75,0.065,0.68)
            for i in 0..<3 {
                let x = 0.08+Double(i)*0.43, y = 0.61+Double(i%2)*0.12, height = 0.12+Double(i%2)*0.045
                line(x,y,x+0.014,y-height*0.74,0.014,0x365644,0.56)
                line(x+0.012,y-height*0.6,x-0.075,y-height*0.85,0.012,0x365644,0.56)
                line(x+0.012,y-height*0.66,x+0.10,y-height*0.92,0.012,0x365644,0.56)
                oval(x-0.18,y-height,0.38,height*0.3,0x365644,0.56); oval(x-0.09,y-height*1.10,0.26,height*0.3,0x365644,0.56)
            }
            stone(0.73,0.79,0.19,0x647B6C); stone(0.86,0.81,0.13,0x526C60); haze(0xBAC5AB,0.56,0.13)
            for i in 0..<32 { grass(Double(i*37%103)/102,0.68+Double(i*17%26)/100,0.018+Double(i%3)*0.005,0xA5AC7D,0.47,i) }
        }
    }
}
