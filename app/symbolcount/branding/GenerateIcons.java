import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Reproducible shape-count master; no font or third-party rendering dependencies. Run from repository root. */
public class GenerateIcons {
    static final Path ROOT = Path.of("app/symbolcount");
    static void symbols(Graphics2D g) {
        g.fill(new Ellipse2D.Double(300,280,170,170));
        g.fill(new Rectangle2D.Double(554,280,170,170));
        Path2D triangle = new Path2D.Double(); triangle.moveTo(385,500); triangle.lineTo(480,675);
        triangle.lineTo(290,675); triangle.closePath(); g.fill(triangle);
        Path2D diamond = new Path2D.Double(); diamond.moveTo(639,490); diamond.lineTo(739,590);
        diamond.lineTo(639,690); diamond.lineTo(539,590); diamond.closePath(); g.fill(diamond);
        for (int i=0; i<3; i++) g.fill(new Ellipse2D.Double(437+i*60,730,30,30));
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
        String svg="<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1024 1024\"><rect width=\"1024\" height=\"1024\" fill=\"#f0ecff\"/><g fill=\"#6852bd\"><circle cx=\"385\" cy=\"365\" r=\"85\"/><rect x=\"554\" y=\"280\" width=\"170\" height=\"170\"/><path d=\"M385 500L480 675H290Z M639 490L739 590L639 690L539 590Z\"/><circle cx=\"452\" cy=\"745\" r=\"15\"/><circle cx=\"512\" cy=\"745\" r=\"15\"/><circle cx=\"572\" cy=\"745\" r=\"15\"/></g></svg>\n";
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
