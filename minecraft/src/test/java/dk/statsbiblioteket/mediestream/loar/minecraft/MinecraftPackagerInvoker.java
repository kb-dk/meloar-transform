package dk.statsbiblioteket.mediestream.loar.minecraft;

public class MinecraftPackagerInvoker {

    public static void main(String[] args) {
        MinecraftPackager.main(new String[]{
                "minecraft/src/test/resources/input",
                "minecraft/src/test/resources/minecraft_filelist.csv",
                "minecraft/src/test/resources/output"});
    }
}
