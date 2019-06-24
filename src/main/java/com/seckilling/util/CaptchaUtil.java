package com.seckilling.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;


public class CaptchaUtil {
    private static final int width = 90;  // image width
    private static final int height = 20;
    private static final int codeCount = 4; // number of chars on Captcha image
    private static final int fontSize = 18;
    private static final int xStart = 15;
    private static final int yStart = 16;
    private static final char[] codeSequence = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
            'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };


    public static Map<String, Object> generateCodeAndPic() {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics gd = bufferedImage.getGraphics();

        Random random = new Random();
        gd.setColor(Color.WHITE);
        gd.fillRect(0, 0, width, height);

        Font font = new Font("Fixedsys", Font.BOLD, fontSize);
        gd.setFont(font);

        // draw black border
        gd.setColor(Color.BLACK);
        gd.drawRect(0, 0, width - 1, height - 1);

        // draw 30 lines randomly
        gd.setColor(Color.BLACK);
        for (int i = 0; i < 30; i++) {
            int x = random.nextInt(width), y = random.nextInt(height);
            int xl = random.nextInt(12), yl = random.nextInt(12);
            gd.drawLine(x, y, x + xl, y + yl);
        }

        StringBuffer captcha = new StringBuffer();
        int red = 0, green = 0, blue = 0;

        for (int i = 0; i < codeCount; i++) {
            String code = String.valueOf(codeSequence[random.nextInt(codeSequence.length)]);
            red = random.nextInt(255);
            green = random.nextInt(255);
            blue = random.nextInt(255);

            // set the char generated to image
            gd.setColor(new Color(red, green, blue));
            gd.drawString(code, (i + 1) * xStart, yStart);

            captcha.append(code);
        }

        Map<String,Object> resultMap  =new HashMap<>();
        resultMap.put("captcha", captcha);
        resultMap.put("captchaPic", bufferedImage);

        return resultMap;
    }


    public static void main(String[] args) throws Exception {
        OutputStream out = new FileOutputStream("/Users/hlyin/Workspaces/JetBrains/IdeaWorkSpace/Seckilling/"+System.currentTimeMillis()+".jpg");
        Map<String,Object> map = CaptchaUtil.generateCodeAndPic();
        ImageIO.write((RenderedImage) map.get("captchaPic"), "jpeg", out);
        System.out.println("Captcha：" + map.get("captcha"));
    }

}
