/*
 * This file is part of ViaProxyAuthHook - https://github.com/ViaVersionAddons/ViaProxyAuthHook
 * Copyright (C) 2024-2025 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.lenni0451.authhook;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Map;

public class URLRedirector implements ClassFileTransformer {

    private static final String URL = "https://sessionserver.mojang.com";

    private final String targetAddress;
    private final String secretKey;

    public URLRedirector(final Map<String, String> config) {
        this.targetAddress = this.formatURL(config.get(Config.TARGET_ADDRESS));
        this.secretKey = config.get(Config.SECRET_KEY);
    }

    private String formatURL(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("Invalid URL (missing protocol): " + url);
        }
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        try {
            ClassNode node = this.read(classfileBuffer);
            boolean modified = false;
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String) {
                        LdcInsnNode ldc = (LdcInsnNode) insn;
                        String str = (String) ldc.cst;
                        if (str.startsWith(URL)) {
                            str = str.substring(URL.length());
                            str = this.targetAddress + "/" + this.secretKey + str;
                            ldc.cst = str;

                            modified = true;
                            System.out.println("Redirected Auth URL in class '" + node.name + "' method '" + method.name + "'");
                        }
                    }
                }
            }
            return modified ? this.write(node) : null;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private ClassNode read(final byte[] bytes) {
        ClassNode node = new ClassNode();
        ClassReader reader = new ClassReader(bytes);
        reader.accept(node, ClassReader.EXPAND_FRAMES);
        return node;
    }

    private byte[] write(final ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

}
