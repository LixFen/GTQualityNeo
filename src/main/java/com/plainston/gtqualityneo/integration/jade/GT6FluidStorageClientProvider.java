package com.plainston.gtqualityneo.integration.jade;

import java.util.List;

import net.minecraft.resources.Identifier;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.ViewGroup;

public enum GT6FluidStorageClientProvider implements IClientExtensionProvider<FluidView.Data, FluidView> {
    INSTANCE;
    @Override public Identifier getUid() { return GT6FluidStorageProvider.UID; }

    @Override public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<FluidView.Data>> groups) {
        return ClientViewGroup.map(groups, FluidView::readDefault, null);
    }
}
