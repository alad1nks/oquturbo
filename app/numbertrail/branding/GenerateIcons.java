import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Reproducible numbered-tile master; no font or third-party rendering dependencies. Run from repository root. */
public class GenerateIcons {
    static final String[] NUMBERS = {"M 18 24 L 30 14 L 30 58 M 18 58 L 42 58", "M 14 24 Q 16 10 32 14 Q 52 20 36 34 L 14 58 L 48 58", "M 14 16 L 46 16 L 28 34 Q 52 32 46 48 Q 38 66 14 54", "M 38 60 L 38 14 L 12 44 L 50 44"};
    static final Path ROOT = Path.of("app/numbertrail");
    static Shape path(String d) {
        String[] words = d.split(" "); Path2D.Double p = new Path2D.Double(); int i = 0;
        while (i < words.length) {
            String op = words[i++]; double x = Double.parseDouble(words[i++]); double y = Double.parseDouble(words[i++]);
            if (op.equals("M")) p.moveTo(x,y); else if (op.equals("L")) p.lineTo(x,y);
            else { double x2=Double.parseDouble(words[i++]), y2=Double.parseDouble(words[i++]); p.quadTo(x,y,x2,y2); }
        }
        return p;
    }
    static void render(Path file, int size, boolean foreground, boolean mono) throws Exception {
        BufferedImage image = new BufferedImage(size * 4, size * 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics(); g.scale(size * 4 / 1024.0, size * 4 / 1024.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (!foreground) { g.setColor(new Color(0xF0ECFF)); g.fillRect(0,0,1024,1024); }
        g.setColor(mono ? Color.BLACK : new Color(0xA5A0E8)); g.setStroke(new BasicStroke(20,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(372,372,652,372)); g.draw(new Line2D.Double(652,372,372,652)); g.draw(new Line2D.Double(372,652,652,652));
        for (int i=0;i<4;i++) {
            int x=252+(i%2)*280, y=252+(i/2)*280;
            g.setColor(mono ? Color.BLACK : new Color(i==3?0x4258AE:0x6852BD));
            g.fill(new RoundRectangle2D.Double(x,y,240,240,50,50));
            Graphics2D n=(Graphics2D)g.create(); n.translate(x+48,y+25); n.scale(2.5,2.5);
            n.setStroke(new BasicStroke(6,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            if (mono) n.setComposite(AlphaComposite.Clear); else n.setColor(Color.WHITE);
            n.draw(path(NUMBERS[i])); n.dispose();
        }
        g.dispose(); BufferedImage output=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB); Graphics2D down=output.createGraphics();
        down.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC); down.drawImage(image,0,0,size,size,null); down.dispose();
        Files.createDirectories(file.getParent()); ImageIO.write(output,"png",file.toFile());
    }
    public static void main(String[] args) throws Exception {
        StringBuilder svg=new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1024 1024\"><rect width=\"1024\" height=\"1024\" fill=\"#f0ecff\"/><path d=\"M372 372H652L372 652H652\" fill=\"none\" stroke=\"#a5a0e8\" stroke-width=\"20\"/>");
        for(int i=0;i<4;i++){int x=252+i%2*280,y=252+i/2*280; svg.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"240\" height=\"240\" rx=\"50\" fill=\"").append(i==3?"#4258ae":"#6852bd").append("\"/><path transform=\"translate(").append(x+48).append(" ").append(y+25).append(") scale(2.5)\" d=\"").append(NUMBERS[i]).append("\" fill=\"none\" stroke=\"white\" stroke-width=\"6\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>");}
        Files.writeString(ROOT.resolve("branding/icon-master.svg"),svg.append("</svg>\n").toString());
        render(ROOT.resolve("icon-master-1024.png"),1024,false,false);
        render(ROOT.resolve("androidApp/src/main/ic_launcher-playstore.png"),512,false,false);
        String[] densities={"mdpi","hdpi","xhdpi","xxhdpi","xxxhdpi"}; double[] scales={1,1.5,2,3,4};
        for(int i=0;i<5;i++) {Path dir=ROOT.resolve("androidApp/src/main/res/mipmap-"+densities[i]);render(dir.resolve("ic_launcher.png"),(int)(48*scales[i]),false,false);render(dir.resolve("ic_launcher_foreground.png"),(int)(108*scales[i]),true,false);render(dir.resolve("ic_launcher_monochrome.png"),(int)(108*scales[i]),true,true);}
        render(ROOT.resolve("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png"),1024,false,false);
        render(ROOT.resolve("desktopApp/src/main/resources/icon.png"),256,false,false);
        render(ROOT.resolve("webApp/src/webMain/resources/icon.png"),192,false,false);
    }
}
