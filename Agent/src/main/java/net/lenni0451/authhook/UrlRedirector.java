/*
 * This file is part of ViaProxyAuthHook - https://github.com/ViaVersionAddons/ViaProxyAuthHook
 * Copyright (C) 2024-2026 RK_01/RaphiMC and contributors
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
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.ClassFileTransformer;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.Base64;
import java.util.Map;

public class UrlRedirector implements ClassFileTransformer {

    private static final String[] URLS = {"https://sessionserver.mojang.com", "https://discovery.minecraftservices.com"};

    private final String targetAddress;
    private final String secretKey;

    public UrlRedirector(final Map<String, String> config) {
        this.targetAddress = this.formatUrl(config.get(Config.TARGET_ADDRESS));
        this.secretKey = config.get(Config.SECRET_KEY);
    }

    private String formatUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("Invalid URL (missing protocol): " + url);
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) {
        try {
            final ClassNode node = this.read(classfileBuffer);
            boolean modified = false;
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof LdcInsnNode) {
                        final LdcInsnNode ldcInsn = (LdcInsnNode) insn;
                        if (ldcInsn.cst instanceof String) {
                            final String original = (String) ldcInsn.cst;
                            final String replacement = this.redirectUrl(original);
                            if (replacement != null) {
                                System.out.println("Redirected '" + original + "' in class '" + node.name + "' method '" + method.name + "'");
                                ldcInsn.cst = replacement;
                                modified = true;
                            }
                        }
                    } else if (insn instanceof InvokeDynamicInsnNode) {
                        final InvokeDynamicInsnNode invokeDynamicInsn = (InvokeDynamicInsnNode) insn;
                        for (int i = 0; i < invokeDynamicInsn.bsmArgs.length; i++) {
                            if (invokeDynamicInsn.bsmArgs[i] instanceof String) {
                                final String original = (String) invokeDynamicInsn.bsmArgs[i];
                                final String replacement = this.redirectUrl(original);
                                if (replacement != null) {
                                    System.out.println("Redirected '" + original + "' in class '" + node.name + "' method '" + method.name + "'");
                                    invokeDynamicInsn.bsmArgs[i] = replacement;
                                    modified = true;
                                }
                            }
                        }
                    }
                }
            }
            return modified ? this.write(node) : null;
        } catch (final Throwable ignored) {
        }
        return null;
    }

    private ClassNode read(final byte[] bytes) {
        final ClassNode node = new ClassNode();
        final ClassReader reader = new ClassReader(bytes);
        reader.accept(node, ClassReader.EXPAND_FRAMES);
        return node;
    }

    private byte[] write(final ClassNode node) {
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private String redirectUrl(final String str) {
        for (String url : URLS) {
            if (str.startsWith(url)) {
                return this.targetAddress + "/" + this.secretKey + "/" + Base64.getUrlEncoder().encodeToString(url.getBytes(StandardCharsets.UTF_8)) + str.substring(url.length());
            }
        }
        return null;
    }

}
