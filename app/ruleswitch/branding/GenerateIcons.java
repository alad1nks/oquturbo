import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Reproducible opposing-rule arrows master; no font or third-party rendering dependencies. Run from repository root. */
public class GenerateIcons {
    static final Path ROOT = Path.of("app/ruleswitch");
    static void symbols(Graphics2D g) {
        g.setStroke(new BasicStroke(36, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Rectangle2D.Double(300,330,110,110));
        g.draw(new Line2D.Double(445,385,724,385));
        g.draw(new Line2D.Double(655,316,724,385));
        g.draw(new Line2D.Double(655,454,724,385));
        g.draw(new Ellipse2D.Double(614,584,110,110));
        g.draw(new Line2D.Double(300,639,579,639));
        g.draw(new Line2D.Double(369,570,300,639));
        g.draw(new Line2D.Double(369,708,300,639));
    }
    static void render(Path file, int size, boolean foreground, boolean mono) throws Exception {
        BufferedImage image = new BufferedImage(size * 4, size * 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics(); g.scale(size * 4 / 1024.0, size * 4 / 1024.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (!foreground) { g.setColor(new Color(0xF0ECFF)); g.fillRect(0,0,1024,1024); }
        g.setColor(mono ? Color.BLACK : new Color(0x6852BD));
        symbols(g);
        g.dispose(); BufferedImage output=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB); Graphics2D down=output.createGraphics();
        down.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC); down.drawImage(image,0,0,size,size,null); down.dispose();
        Files.createDirectories(file.getParent()); ImageIO.write(output,"png",file.toFile());
    }
    public static void main(String[] args) throws Exception {
        String svg="<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1024 1024\"><rect width=\"1024\" height=\"1024\" fill=\"#f0ecff\"/><g fill=\"none\" stroke=\"#6852bd\" stroke-width=\"36\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect x=\"300\" y=\"330\" width=\"110\" height=\"110\"/><path d=\"M445 385H724L655 316M724 385L655 454M579 639H300L369 570M300 639L369 708\"/><circle cx=\"669\" cy=\"639\" r=\"55\"/></g></svg>\n";
        Files.writeString(ROOT.resolve("branding/icon-master.svg"),svg);
        render(ROOT.resolve("icon-master-1024.png"),1024,false,false);
        render(ROOT.resolve("androidApp/src/main/ic_launcher-playstore.png"),512,false,false);
        String[] densities={"mdpi","hdpi","xhdpi","xxhdpi","xxxhdpi"}; double[] scales={1,1.5,2,3,4};
        for(int i=0;i<5;i++) {Path dir=ROOT.resolve("androidApp/src/main/res/mipmap-"+densities[i]);render(dir.resolve("ic_launcher.png"),(int)(48*scales[i]),false,false);render(dir.resolve("ic_launcher_foreground.png"),(int)(108*scales[i]),true,false);render(dir.resolve("ic_launcher_monochrome.png"),(int)(108*scales[i]),true,true);}
        render(ROOT.resolve("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png"),1024,false,false);
        render(ROOT.resolve("desktopApp/src/main/resources/icon.png"),256,false,false);
        render(ROOT.resolve("webApp/src/webMain/resources/icon.png"),192,false,false);
    }
}
