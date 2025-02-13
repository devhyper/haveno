/*
 * This file is part of Haveno.
 *
 * Haveno is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Haveno is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Haveno. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.payment.payload;

import bisq.core.locale.Res;

import com.google.protobuf.Message;

import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
@Getter
@Slf4j
public final class UpiAccountPayload extends CountryBasedPaymentAccountPayload {
    private String virtualPaymentAddress = "";

    public UpiAccountPayload(String paymentMethod, String id) {
        super(paymentMethod, id);
    }

    private UpiAccountPayload(String paymentMethod,
                                String id,
                                String countryCode,
                                String virtualPaymentAddress,
                                long maxTradePeriod,
                                Map<String, String> excludeFromJsonDataMap) {
        super(paymentMethod,
                id,
                countryCode,
                maxTradePeriod,
                excludeFromJsonDataMap);

        this.virtualPaymentAddress = virtualPaymentAddress;
    }

    @Override
    public Message toProtoMessage() {
        protobuf.UpiAccountPayload.Builder builder = protobuf.UpiAccountPayload.newBuilder()
                .setVirtualPaymentAddress(virtualPaymentAddress);
        final protobuf.CountryBasedPaymentAccountPayload.Builder countryBasedPaymentAccountPayload = getPaymentAccountPayloadBuilder()
                .getCountryBasedPaymentAccountPayloadBuilder()
                .setUpiAccountPayload(builder);
        return getPaymentAccountPayloadBuilder()
                .setCountryBasedPaymentAccountPayload(countryBasedPaymentAccountPayload)
                .build();
    }

    public static UpiAccountPayload fromProto(protobuf.PaymentAccountPayload proto) {
        protobuf.CountryBasedPaymentAccountPayload countryBasedPaymentAccountPayload = proto.getCountryBasedPaymentAccountPayload();
        protobuf.UpiAccountPayload upiAccountPayloadPB = countryBasedPaymentAccountPayload.getUpiAccountPayload();
        return new UpiAccountPayload(proto.getPaymentMethodId(),
                proto.getId(),
                countryBasedPaymentAccountPayload.getCountryCode(),
                upiAccountPayloadPB.getVirtualPaymentAddress(),
                proto.getMaxTradePeriod(),
                new HashMap<>(proto.getExcludeFromJsonDataMap()));
    }

    @Override
    public String getPaymentDetails() {
        return Res.get(paymentMethodId) + " - " + Res.getWithCol("payment.upi.virtualPaymentAddress") + " " + virtualPaymentAddress;
    }

    @Override
    public String getPaymentDetailsForTradePopup() {
        return getPaymentDetails();
    }

    @Override
    public byte[] getAgeWitnessInputData() {
        return super.getAgeWitnessInputData(virtualPaymentAddress.getBytes(StandardCharsets.UTF_8));
    }
}
