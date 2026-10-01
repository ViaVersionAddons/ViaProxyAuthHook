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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class UrlRedirector implements ClassFileTransformer {

    private static final String URL = "https://sessionserver.mojang.com";

    private static final String DISCOVERY_SERVICE_CLASS = "MinecraftServicesDiscoveryService";
    private static final String GET_URL_METHOD = "getUrl";

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
        final boolean isDiscoveryService = className != null && className.endsWith(DISCOVERY_SERVICE_CLASS);
        try {
            final ClassNode node = this.read(classfileBuffer);
            boolean modified = false;

            for (MethodNode method : node.methods) {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String) {
                        final LdcInsnNode ldc = (LdcInsnNode) insn;
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

            if (isDiscoveryService) {
                modified |= this.patchDiscoveryService(node);
            }

            return modified ? this.write(node) : null;
        } catch (final Throwable t) {
            if (isDiscoveryService) {
                System.err.println("Failed to transform '" + className + "'");
                t.printStackTrace();
            }
        }
        return null;
    }

    private boolean patchDiscoveryService(final ClassNode node) {
        final String replacement = this.targetAddress + "/" + this.secretKey;
        boolean modified = false;

        for (MethodNode method : node.methods) {
            if (!GET_URL_METHOD.equals(method.name)) {
                continue;
            }
            if (!method.desc.endsWith(")Ljava/lang/String;")) {
                continue;
            }

            boolean methodModified = false;
            for (AbstractInsnNode insn : method.instructions) {
                if (insn.getOpcode() == ARETURN) {
                    final InsnList inject = new InsnList();
                    inject.add(new LdcInsnNode(URL));
                    inject.add(new LdcInsnNode(replacement));
                    inject.add(new MethodInsnNode(
                        INVOKEVIRTUAL,
                        "java/lang/String",
                        "replace",
                        "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;",
                        false
                    ));
                    method.instructions.insertBefore(insn, inject);
                    methodModified = true;
                }
            }

            if (methodModified) {
                modified = true;
                System.out.println("Patched discovery getUrl in class '" + node.name + "' method '" + method.name + method.desc + "'");
            }
        }
        return modified;
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

}
