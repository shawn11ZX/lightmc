package com.wooduan.lightmc.serializer.jobj;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.wooduan.lightmc.APC;
import com.wooduan.lightmc.serializer.ApcMigration;

public class HackedObjectOutputStream extends ObjectOutputStream {

	/**
	 * Migration table. Holds old to new classes representation.
	 */
	private static final Map<Class<?>, String> MIGRATION_MAP = new HashMap<Class<?>, String>();

	private static Field nameField;
	static {
		MIGRATION_MAP.put(APC.class, ApcMigration.JOBJ_NAME);
		try {
			nameField = ObjectStreamClass.class.getDeclaredField("name");
			nameField.setAccessible(true);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	
	 
	public HackedObjectOutputStream(final OutputStream stream)
			throws IOException {
		super(stream);
	}

	@Override
	protected void writeClassDescriptor(ObjectStreamClass desc)
			throws IOException {

		for (final Class oldName : MIGRATION_MAP.keySet()) {
			if (desc.getName().equals(oldName.getName())) {
				String replacement = MIGRATION_MAP.get(oldName);
				try {
					nameField.set(desc, replacement);
				} catch (Exception e) {
					throw new IllegalArgumentException(e);
				}

			}
		}
		super.writeClassDescriptor(desc);
	}
}
